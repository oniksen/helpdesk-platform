package login

/** Значения для формы входа. Не используется для [LoginMode.Silent] */
data class LoginCredentials(
    val email: String,
    val password: String
)
