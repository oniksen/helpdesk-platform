package domain.intent

sealed interface SettingsIntent {
    data object Logout : SettingsIntent
}
