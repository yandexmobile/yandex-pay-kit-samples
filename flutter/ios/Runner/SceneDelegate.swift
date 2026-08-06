import Flutter
import UIKit
import YandexPayConfiguration

class SceneDelegate: FlutterSceneDelegate {

    override func scene(
        _ scene: UIScene,
        openURLContexts URLContexts: Set<UIOpenURLContext>
    ) {
        super.scene(scene, openURLContexts: URLContexts)
        guard let url = URLContexts.first?.url else { return }
        Task { @MainActor in
            guard YPay.isInitialized else { return }
            _ = YPay.instance.deeplinkHandler.handleOpenURL(url)
        }
    }

    override func scene(
        _ scene: UIScene,
        continue userActivity: NSUserActivity
    ) {
        super.scene(scene, continue: userActivity)
        Task { @MainActor in
            guard YPay.isInitialized else { return }
            _ = YPay.instance.deeplinkHandler.handleUserActivity(userActivity)
        }
    }
}
