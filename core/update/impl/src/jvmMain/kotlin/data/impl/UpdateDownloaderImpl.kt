package data.impl

import DownloadStatus
import KtorClient
import data.UpdateDataSource
import domain.models.channels.ChannelsFile
import domain.models.manifest.v2.UpdateManifest
import domain.port.UpdateDownloader
import kotlinx.coroutines.flow.Flow

/**
 * Реализация [UpdateDownloader] для JVM-приложения.
 *
 * Манифест читается существующим v4-источником [UpdateDataSource] и на клиенте
 * не кэшируется: каждый вызов [downloadUpdateManifest] — новый запрос.
 * Скачивание архива делегируется слою сети.
 */
class UpdateDownloaderImpl(
    private val dataSource: UpdateDataSource,
    private val client: KtorClient,
) : UpdateDownloader {

    override suspend fun downloadUpdateManifest(): UpdateManifest? =
        runCatching { dataSource.fetchManifest() }
            .onFailure { error ->
                println("[UpdateDownloader] Манифест недоступен: ${error.message}")
            }
            .getOrNull()

    override suspend fun downloadChannelsManifest(): ChannelsFile? =
        runCatching { dataSource.fetchChannelsFile() }
            .onFailure { error ->
                println("[UpdateDownloader] Файл каналов недоступен: ${error.message}")
            }
            .getOrNull()

    override suspend fun downloadWithProgress(
        url: String,
        destinationFilePath: String,
    ): Flow<DownloadStatus> = client.downloadFileWithProgress(url, destinationFilePath)
}
