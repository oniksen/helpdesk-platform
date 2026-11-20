/**
 * Статус потоковой загрузки файла.
 *
 * Эмитируется через [kotlinx.coroutines.flow.Flow] при вызове
 * [KtorClient.downloadFileWithProgress].
 * */
sealed interface DownloadStatus {
    /**
     * Промежуточный прогресс загрузки.
     *
     * @property percentage доля загруженного от общего размера файла, 0.0..1.0.
     * */
    data class Progress(val percentage: Float) : DownloadStatus

    /**
     * Загрузка успешно завершена.
     *
     * @property filePath путь к скачанному файлу.
     * */
    data class Success(val filePath: String) : DownloadStatus
}
