/**
 * Первичный адрес установки: канал, из которого приложение забирает сборку,
 * когда манифест не дал ссылку, то есть при установке в новом браузере.
 * */
// TODO("Заменить дефолтную ссылку первичной установки с canary на prod,
// когда релизный канал будет публиковаться всегда свежей сборкой.")
const val CANARY_WEB_BUILD_URL = "https://helpdesk.lpmti.ru/helpdesk-app/v2/channels/dev/canary/web/web-canary.zip"

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
        url: String,
        forceRefresh: Boolean = false,
    ): Map<String, ByteArray>

    suspend fun getCachedBuild(url: String): Map<String, ByteArray>?
    suspend fun clearCache()
}
