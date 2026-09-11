package presentation.state

data class AuthorizationState(
    val authResultMessage: String? = null,
    val inProgress: Boolean = true,
)