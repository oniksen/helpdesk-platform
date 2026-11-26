package domain.intent

import login.LoginCredentials

sealed class AuthorizationScreenIntent {
    data class Authorize(val credentials: LoginCredentials? = null) : AuthorizationScreenIntent()
}