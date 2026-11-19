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
 * Манифест и файл каналов читаются существующим v4-источником [UpdateDataSource]
 * и на клиенте не кэшируются: каждый вызов — новый запрос.
 * Ошибки не глушатся: причина нужна менеджеру для состояния проверки.
 * Скачивание архива делегируется слою сети.
 */
class UpdateDownloaderImpl(
    private val dataSource: UpdateDataSource,
    private val client: KtorClient,
) : UpdateDownloader {

    override suspend fun downloadUpdateManifest(): UpdateManifest = dataSource.fetchManifest()

    override suspend fun downloadChannelsManifest(): ChannelsFile = dataSource.fetchChannelsFile()

    override suspend fun downloadWithProgress(
        url: String,
        destinationFilePath: String,
    ): Flow<DownloadStatus> = client.downloadFileWithProgress(url, destinationFilePath)
}
