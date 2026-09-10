package domain.intent

sealed class AuthorizationScreenIntent {
    object OpenHomeScreen : AuthorizationScreenIntent()
}