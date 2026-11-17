import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

interface KtorClient {
    fun baseInstance(): HttpClient

    /**
     * Скачивает файл по [url] в [destinationFilePath], эмитируя прогресс.
     *
     * Прогресс отдаётся долей 0.0..1.0 от общего размера файла; завершение —
     * [DownloadStatus.Success] с путём к файлу.
     * */
    suspend fun downloadFileWithProgress(
        url: String,
        destinationFilePath: String,
    ): Flow<DownloadStatus>
}
