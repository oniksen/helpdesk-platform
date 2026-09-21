package domain.intent

sealed class AuthorizationScreenIntent {
    object Authorize : AuthorizationScreenIntent()
}