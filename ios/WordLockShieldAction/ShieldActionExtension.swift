import ManagedSettings
import UserNotifications

/// Handles the buttons on the shield. iOS does not let a shield open an app directly,
/// so "퀴즈 풀기" posts a notification that opens Word Lock straight into the quiz.
class ShieldActionExtension: ShieldActionDelegate {
    override func handle(action: ShieldAction, for application: ApplicationToken, completionHandler: @escaping (ShieldActionResponse) -> Void) {
        respond(to: action, completionHandler)
    }

    override func handle(action: ShieldAction, for webDomain: WebDomainToken, completionHandler: @escaping (ShieldActionResponse) -> Void) {
        respond(to: action, completionHandler)
    }

    override func handle(action: ShieldAction, for category: ActivityCategoryToken, completionHandler: @escaping (ShieldActionResponse) -> Void) {
        respond(to: action, completionHandler)
    }

    private func respond(to action: ShieldAction, _ done: @escaping (ShieldActionResponse) -> Void) {
        switch action {
        case .primaryButtonPressed:
            AppGroup.defaults.set(true, forKey: Keys.pendingQuiz)
            let content = UNMutableNotificationContent()
            content.title = "Word Lock"
            content.body = "여기를 눌러 영어 퀴즈를 풀어요"
            content.sound = .default
            let request = UNNotificationRequest(identifier: "quiz", content: content, trigger: nil)
            UNUserNotificationCenter.current().add(request) { _ in done(.defer) }
        case .secondaryButtonPressed:
            done(.close)
        @unknown default:
            done(.close)
        }
    }
}
