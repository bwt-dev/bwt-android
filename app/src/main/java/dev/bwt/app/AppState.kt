package dev.bwt.app

class AppState {
    companion object {
        var daemonIsRunning = false
            private set
        var daemonWasRunning = false
            private set
        var pendingSettingsUpdate = false
            private set

        fun setIsRunning() {
            daemonIsRunning = true
            daemonWasRunning = true
        }
        fun setNotRunning() {
            daemonIsRunning = false
        }

        fun setPendingSettingsUpdate() {
            pendingSettingsUpdate = true
        }
        fun checkSettingsChanged(): Boolean {
            val changed = pendingSettingsUpdate
            pendingSettingsUpdate = false
            return changed
        }
    }
}