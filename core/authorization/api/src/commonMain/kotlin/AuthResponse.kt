sealed class AuthResponse {
    data class Success(
        val cookie: String,
        val bearerToken: String,
    ) : AuthResponse()
    data class Error(
        val message: String,
        val errorCode: String? = null
    ) : AuthResponse()
}