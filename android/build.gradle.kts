// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("updateReadmeVersions") {
    doLast {
        val version = libs.versions.ypay.get()
        listOf("README.md").forEach { file ->
            val content = file(file).readText()
                .replace(Regex("""version=\d+\.\d+\.\d+"""), "version=$version")
            file(file).writeText(content)
        }
    }
}
