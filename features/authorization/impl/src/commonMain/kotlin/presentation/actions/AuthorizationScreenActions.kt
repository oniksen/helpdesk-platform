package presentation.actions

import login.LoginCredentials

data class AuthorizationScreenActions(
    val authorize: (credentials: LoginCredentials?, autoLogin: Boolean) -> Unit,
)
