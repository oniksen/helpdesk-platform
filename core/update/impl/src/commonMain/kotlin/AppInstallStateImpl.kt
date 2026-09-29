expect class AppInstallStateImpl : AppInstallState {
    override suspend fun hasNewVersion(): UpdateDecision
}
