package login

import AppException
import AuthResponse
import Authorization

class CredentialsLoginStrategy(
    private val platformAuthorization: Authorization,
) : LoginStrategy {
    override val mode = LoginMode.Credentials

    override suspend fun authorize(credentials: LoginCredentials?): AuthResponse {
        val credentials = try {
            requireNotNull(credentials) {
                "Email или пароль не могут быть null"
            }
        } catch (e: IllegalArgumentException) {
            throw AppException.LoginError.IncorrectCredentials(
                message = e.message,
                cause = e
            )
        }

        return platformAuthorization.helpdeskAuth(
            email = credentials.email,
            password = credentials.password,
        )
    }
}
