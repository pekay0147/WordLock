import SwiftUI
import FamilyControls
import DeviceActivity
import UserNotifications

@MainActor
final class AppModel: ObservableObject {
    private let defaults = AppGroup.defaults

    @Published var authorized = AuthorizationCenter.shared.authorizationStatus == .approved
    @Published var authError: String?
    @Published var selection = Shield.selection
    @Published var unlockUntil: Date?
    @Published var showQuiz = false

    @Published var level: Level { didSet { defaults.set(level.rawValue, forKey: "level") } }
    @Published var minutes: Int { didSet { defaults.set(minutes, forKey: "minutes") } }
    @Published private(set) var todayCount = 0
    @Published private(set) var streak = 0
    @Published private(set) var log: [String] = []

    init() {
        level = Level(rawValue: AppGroup.defaults.string(forKey: "level") ?? "") ?? .easy
        let m = AppGroup.defaults.integer(forKey: "minutes")
        minutes = [15, 30, 60].contains(m) ? m : 15
        unlockUntil = AppGroup.defaults.object(forKey: Keys.unlockUntil) as? Date
        loadStats()
    }

    var blockedCount: Int {
        selection.applicationTokens.count + selection.categoryTokens.count + selection.webDomainTokens.count
    }

    func isUnlocked(at now: Date = .now) -> Bool {
        guard let u = unlockUntil else { return false }
        return u > now
    }

    // MARK: permissions

    func requestAuthorization() async {
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            authError = nil
        } catch {
            authError = "스크린타임 권한을 받지 못했어요. 설정 → 스크린타임이 켜져 있는지 확인하고 다시 눌러 주세요."
        }
        authorized = AuthorizationCenter.shared.authorizationStatus == .approved
        if authorized {
            _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])
        }
    }

    // MARK: blocking

    func saveSelection() {
        Shield.selection = selection
        if !isUnlocked() { Shield.lock() }
    }

    /// Called when the app comes to the foreground or a quiz notification is tapped.
    func refresh() {
        authorized = AuthorizationCenter.shared.authorizationStatus == .approved
        unlockUntil = defaults.object(forKey: Keys.unlockUntil) as? Date
        if let u = unlockUntil, u <= .now { lockNow() }
        loadStats()
        if defaults.bool(forKey: Keys.pendingQuiz) {
            defaults.set(false, forKey: Keys.pendingQuiz)
            if authorized && !isUnlocked() { showQuiz = true }
        }
    }

    func unlock() {
        let end = Date.now.addingTimeInterval(TimeInterval(minutes * 60))
        Shield.unlock()
        unlockUntil = end
        defaults.set(end, forKey: Keys.unlockUntil)

        // Ask iOS to call our monitor extension when the time is up, so the apps lock again.
        let center = DeviceActivityCenter()
        center.stopMonitoring([.unlockWindow])
        let parts: Set<Calendar.Component> = [.year, .month, .day, .hour, .minute, .second]
        let schedule = DeviceActivitySchedule(
            intervalStart: Calendar.current.dateComponents(parts, from: .now),
            intervalEnd: Calendar.current.dateComponents(parts, from: end),
            repeats: false
        )
        try? center.startMonitoring(.unlockWindow, during: schedule)
    }

    func lockNow() {
        DeviceActivityCenter().stopMonitoring([.unlockWindow])
        Shield.lock()
        unlockUntil = nil
    }

    // MARK: stats

    private func rollDay() {
        let today = Calendar.current.startOfDay(for: .now).timeIntervalSince1970
        if defaults.double(forKey: "day") != today {
            defaults.set(today, forKey: "day")
            defaults.set(0, forKey: "count")
            defaults.set(0, forKey: "streak")
            defaults.set([String](), forKey: "log")
        }
    }

    private func loadStats() {
        rollDay()
        todayCount = defaults.integer(forKey: "count")
        streak = defaults.integer(forKey: "streak")
        log = defaults.stringArray(forKey: "log") ?? []
    }

    func recordSolved(_ word: Word, usedHint: Bool) {
        rollDay()
        defaults.set(defaults.integer(forKey: "count") + 1, forKey: "count")
        defaults.set(usedHint ? 0 : defaults.integer(forKey: "streak") + 1, forKey: "streak")
        let time = Date.now.formatted(date: .omitted, time: .shortened)
        let entry = [word.picture, word.english, word.korean, time].joined(separator: "\t")
        defaults.set(Array(([entry] + (defaults.stringArray(forKey: "log") ?? [])).prefix(50)), forKey: "log")
        loadStats()
    }

    func resetStreak() {
        rollDay()
        defaults.set(0, forKey: "streak")
        loadStats()
    }
}
