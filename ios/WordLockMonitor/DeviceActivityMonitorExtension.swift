import DeviceActivity
import ManagedSettings

/// Locks the chosen apps again when the unlock window ends, even if Word Lock isn't running.
class DeviceActivityMonitorExtension: DeviceActivityMonitor {
    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        if activity == .unlockWindow { Shield.lock() }
    }
}
