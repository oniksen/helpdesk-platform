package presentation.actions

import login.LoginCredentials

data class AuthorizationScreenActions(
    val authorize: (credentials: LoginCredentials?) -> Unit,
)