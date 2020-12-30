package dev.bwt.app

class AppState {
    companion object {
        var pendingSettingsUpdate = false
            private set

        fun setSettingsChanged() {
            pendingSettingsUpdate = true
        }
        fun checkSettingsChanged(): Boolean {
            val changed = pendingSettingsUpdate
            pendingSettingsUpdate = false
            return changed
        }
    }
}