package auth

data class AuthData(
    val maxInitData: String,
    val token: String,
    val cookies: String?,
)
