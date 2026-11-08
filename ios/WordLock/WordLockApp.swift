import SwiftUI
import UserNotifications

@main
struct WordLockApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .onChange(of: scenePhase) { phase in
                    if phase == .active { model.refresh() }
                }
                .onReceive(NotificationCenter.default.publisher(for: .openQuiz)) { _ in
                    model.refresh()
                }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        // Shaking changes the word; don't let iOS show its "Undo typing" alert instead.
        application.applicationSupportsShakeToEdit = false
        return true
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        AppGroup.defaults.set(true, forKey: Keys.pendingQuiz)
        NotificationCenter.default.post(name: .openQuiz, object: nil)
        completionHandler()
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound])
    }
}

extension Notification.Name {
    static let openQuiz = Notification.Name("wordlock.openQuiz")
    static let deviceShaken = Notification.Name("wordlock.deviceShaken")
}

extension UIWindow {
    open override func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            NotificationCenter.default.post(name: .deviceShaken, object: nil)
        }
        super.motionEnded(motion, with: event)
    }
}
