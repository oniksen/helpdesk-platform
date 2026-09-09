class AuthorizationImpl(
    private val maxAuthorization: MaxAuthorization
): Authorization {
    override suspend fun getMaxInitData(): String
    = when(val userInitData = maxAuthorization.getInitUserData()) {
        is MaxAuthorizationResult.Success -> userInitData.initData
        is MaxAuthorizationResult.Error -> userInitData.error.cause?.message ?: "Unknown error"
        is MaxAuthorizationResult.Unavailable -> "Max unavailable"
    }
}