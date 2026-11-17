package domain.port

import DownloadStatus
import domain.models.manifest.v2.UpdateManifest
import kotlinx.coroutines.flow.Flow

/**
 * Порт для загрузки обновлений.
 *
 * Определяет контракт для получения манифеста обновления и скачивания
 * архива с отслеживанием прогресса.
 */
interface UpdateDownloader {

    /**
     * Загружает и парсит манифест обновления с сервера.
     *
     * @return [UpdateManifest] с информацией о последней версии, или `null`,
     * если манифест недоступен или произошла ошибка.
     */
    suspend fun downloadUpdateManifest(): UpdateManifest?

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
    ) : Flow<DownloadStatus>
}
