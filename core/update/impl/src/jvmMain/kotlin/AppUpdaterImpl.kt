class AppUpdaterImpl(
    private val appInstallState: AppInstallState
) : AppUpdater {
    override suspend fun checkForUpdate(): UpdateDecision = appInstallState.hasNewVersion()

    override suspend fun downloadAndUnpack(
        url: String,
        forceRefresh: Boolean
    ): Map<String, ByteArray> {
        TODO("Not yet implemented")
    }

    override suspend fun getCachedBuild(url: String): Map<String, ByteArray>? {
        TODO("Not yet implemented")
    }

    override suspend fun clearCache() {
        TODO("Not yet implemented")
    }
}
