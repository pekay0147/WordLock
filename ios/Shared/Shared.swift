import Foundation
import FamilyControls
import ManagedSettings
import DeviceActivity

/// State shared between the app and its Screen Time extensions (same App Group).
enum AppGroup {
    static let id = "group.com.wordlock.app"
    static var defaults: UserDefaults { UserDefaults(suiteName: id) ?? .standard }
}

enum Keys {
    static let selection = "selection"
    static let unlockUntil = "unlockUntil"
    static let pendingQuiz = "pendingQuiz"
}

extension ManagedSettingsStore.Name {
    static let wordLock = Self("wordlock")
}

extension DeviceActivityName {
    /// The window during which blocked apps stay open after a correct answer.
    static let unlockWindow = Self("unlockWindow")
}

enum Shield {
    static let store = ManagedSettingsStore(named: .wordLock)

    /// The apps, categories and websites the user chose to block.
    static var selection: FamilyActivitySelection {
        get {
            guard let data = AppGroup.defaults.data(forKey: Keys.selection),
                  let s = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data)
            else { return FamilyActivitySelection() }
            return s
        }
        set {
            if let data = try? JSONEncoder().encode(newValue) {
                AppGroup.defaults.set(data, forKey: Keys.selection)
            }
        }
    }

    /// Put the quiz shield on every chosen app.
    static func lock() {
        let s = selection
        store.shield.applications = s.applicationTokens.isEmpty ? nil : s.applicationTokens
        store.shield.applicationCategories = s.categoryTokens.isEmpty ? nil : .specific(s.categoryTokens)
        store.shield.webDomains = s.webDomainTokens.isEmpty ? nil : s.webDomainTokens
        AppGroup.defaults.removeObject(forKey: Keys.unlockUntil)
    }

    /// Remove the shield (after a correct answer).
    static func unlock() {
        store.clearAllSettings()
    }
}
