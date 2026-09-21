data class AuthResponse(
    val bearerToken: String,
    val cookies: String?,
    val userId: Int,
    val email: String,
)