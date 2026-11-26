package login

import AuthResponse
import Authorization
import MaxAuthorization

class MaxLoginStrategy(
    private val platformAuthorization: Authorization,
    private val maxAuthorization: MaxAuthorization,
) : LoginStrategy {

    override val mode = LoginMode.Silent

    override suspend fun authorize(credentials: LoginCredentials?): AuthResponse {
        val initData = when (val result = maxAuthorization.getInitUserData()) {
            is MaxAuthorizationResult.Success -> result.initData
            is MaxAuthorizationResult.Error -> throw AppException.LoginError.MaxLoginUnavailable(
                cause = result.error,
            )
            MaxAuthorizationResult.Unavailable -> throw AppException.LoginError.MaxLoginUnavailable()
        }

        return platformAuthorization.helpdeskAuth(initData)
    }
}
