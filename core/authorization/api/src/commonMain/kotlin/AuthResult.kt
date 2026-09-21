sealed class AuthResult {
    data object Idle : AuthResult()
    data object Success : AuthResult()
    data class Error(
        val message: String,
        val errorCode: String? = null
    ) : AuthResult()
}