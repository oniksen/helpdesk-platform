package presentation.state

import login.LoginMode

data class AuthorizationState(
    val authResultMessage: String? = null,
    val inProgress: Boolean = true,
    val loginMode: LoginMode = LoginMode.Silent,
)