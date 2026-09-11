package presentation

sealed class AuthorizationScreenEffect {
    data class ShowSnackBar(val message: String) : AuthorizationScreenEffect()
}
