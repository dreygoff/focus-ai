package app.focus.domain.usecase

import app.focus.domain.model.SettingsShortcut

data class SettingsShortcutTarget(
    val settingsPackage: String,
    val activityClassName: String,
    val intentAction: String,
)

interface SettingsShortcutGateway {
    fun resolve(shortcut: SettingsShortcut): SettingsShortcutTarget?
}
