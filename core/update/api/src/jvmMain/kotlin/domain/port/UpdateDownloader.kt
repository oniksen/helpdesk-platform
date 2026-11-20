package domain.port

import DownloadStatus
import domain.models.channels.ChannelsFile
import domain.models.manifest.v2.UpdateManifest
import kotlinx.coroutines.flow.Flow

/**
 * Порт для загрузки данных обновлений.
 *
 * Манифест и файл каналов бросают исключение при сетевой ошибке,
 * не-2xx ответе или ошибке разбора: вызывающий решает, где нужен
 * fallback, а где — состояние ошибки с причиной.
 */
interface UpdateDownloader {

    /** Загружает и парсит манифест обновления. */
    suspend fun downloadUpdateManifest(): UpdateManifest

    /** Загружает и парсит файл каналов пользователей. */
    suspend fun downloadChannelsManifest(): ChannelsFile

    /**
     * Скачивает файл обновления по URL с отслеживанием прогресса.
     *
     * @param url URL для скачивания архива.
     * @param destinationFilePath Путь, по которому будет сохранён скачанный архив.
     * @return [Flow]<[DownloadStatus]>, эмитирующий прогресс и результат загрузки.
     */
    suspend fun downloadWithProgress(
        url: String,
        destinationFilePath: String
    ): Flow<DownloadStatus>
}
