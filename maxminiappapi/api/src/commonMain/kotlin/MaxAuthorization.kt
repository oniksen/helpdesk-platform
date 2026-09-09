interface MaxAuthorization {
    suspend fun getInitUserData(): MaxAuthorizationResult
}