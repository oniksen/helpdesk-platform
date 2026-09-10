class AuthorizationImpl(
    private val maxAuthorization: MaxAuthorization
): Authorization {
    override suspend fun getMaxInitData(): MaxAuthorizationResult
        = maxAuthorization.getInitUserData()
}