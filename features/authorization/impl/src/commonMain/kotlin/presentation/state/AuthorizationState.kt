package presentation.state

data class AuthorizationState(
    val authResultMessage: String? = null,
    val testBtnEnabled: Boolean = false,
    val inProgress: Boolean = true,
)