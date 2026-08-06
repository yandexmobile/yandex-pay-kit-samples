# frozen_string_literal: true

module YandexPaySdkPodHelpers
  RUNTIME_PHASE_PREFIX = '[CP-User] Embed Yandex'
  RUNTIME_PHASE_SUFFIX = ' Runtime Frameworks'
  PARALLEL_CODESIGN_WAIT = [
    'if [ "${COCOAPODS_PARALLEL_CODE_SIGN}" == "true" ]; then',
    '  wait',
    'fi',
  ].freeze
  FRAMEWORK_CALL_PATTERN = %r{
    printf\x20'\x20\x20
    (install_framework\x20"
      \$\{PODS_XCFRAMEWORKS_BUILD_DIR\}/YandexPaySDK/
      [A-Za-z0-9_]+/[A-Za-z0-9_]+\.framework
    ")\\n'
  }x
  private_constant :RUNTIME_PHASE_PREFIX, :RUNTIME_PHASE_SUFFIX,
                   :PARALLEL_CODESIGN_WAIT, :FRAMEWORK_CALL_PATTERN

  module_function

  def materialize_runtime_frameworks(installer)
    yandex_target = installer.pods_project.targets.find { |target| target.name == 'YandexPaySDK' }
    return unless yandex_target

    runtime_phases = yandex_target.shell_script_build_phases.select do |phase|
      phase.name.start_with?(RUNTIME_PHASE_PREFIX) && phase.name.end_with?(RUNTIME_PHASE_SUFFIX)
    end
    return if runtime_phases.empty?

    framework_scripts = Dir.glob(
      File.join(installer.sandbox.root, 'Target Support Files', 'Pods-*', '*-frameworks.sh')
    )
    raise 'No CocoaPods framework embed scripts found' if framework_scripts.empty?

    runtime_phases.each do |phase|
      materialize_runtime_phase(phase, framework_scripts)
      phase.remove_from_project
    end
  end

  def materialize_runtime_phase(phase, framework_scripts)
    marker_name = phase.shell_script[/# BEGIN ([A-Za-z0-9:]+)/, 1]
    framework_calls = phase.shell_script.scan(FRAMEWORK_CALL_PATTERN).flatten.uniq
    raise "Unable to parse #{phase.name}" unless marker_name && !framework_calls.empty?

    framework_scripts.each do |framework_script|
      contents = File.read(framework_script)
      next if contents.include?("# BEGIN #{marker_name}")

      missing_calls = framework_calls.reject { |call| contents.include?(call) }
      block = ['', "# BEGIN #{marker_name}", *missing_calls.map { |call| "  #{call}" },
               *PARALLEL_CODESIGN_WAIT, "# END #{marker_name}", ''].join("\n")
      File.write(framework_script, contents + block)
    end
  end
  private_class_method :materialize_runtime_phase
end
