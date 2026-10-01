import `helpdesk-platform`.config.BuildKonfig

actual class AppVersionProviderImpl : AppVersionProvider {
    actual override fun provide(): AppVersion =
        BuildKonfig.PROJECT_TECHNICAL_VERSION_WEB.normalizeFromTechnical()
}
