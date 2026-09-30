const val DEFAULT_WEB_APP_URL = "https://helpdesk.lpmti.ru/helpdesk-app/download-web-app-source.php"

interface AppUpdater {
    suspend fun checkForUpdate(): UpdateDecision

    /**
     * Скачать и распаковать сборку приложения.
     *
     * @param url адрес zip-архива сборки.
     * @param forceRefresh игнорировать закэшированную сборку и обратиться к сети.
     * Нужен при установке обновления, иначе в Service Worker уедет уже знакомая сборка.
     * */
    suspend fun downloadAndUnpack(
        url: String = DEFAULT_WEB_APP_URL,
        forceRefresh: Boolean = false,
    ): Map<String, ByteArray>

    suspend fun getCachedBuild(url: String = DEFAULT_WEB_APP_URL): Map<String, ByteArray>?
    suspend fun clearCache()
}
