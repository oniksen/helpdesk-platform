const val DEFAULT_WEB_APP_URL = "https://helpdesk.lpmti.ru/helpdesk-app/webApp.zip"

interface AppUpdater {
    suspend fun downloadAndUnpack(url: String = DEFAULT_WEB_APP_URL): Map<String, ByteArray>
    suspend fun getCachedBuild(url: String = DEFAULT_WEB_APP_URL): Map<String, ByteArray>?
    suspend fun clearCache()
}
