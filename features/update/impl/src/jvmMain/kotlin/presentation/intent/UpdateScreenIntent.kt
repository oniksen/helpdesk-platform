package presentation.intent

sealed class UpdateScreenIntent {
    data object StartUpdate : UpdateScreenIntent()
}
