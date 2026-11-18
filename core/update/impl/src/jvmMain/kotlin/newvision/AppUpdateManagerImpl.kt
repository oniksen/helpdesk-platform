package domain

import AppException
import KtorClient
import data.dto.channels.ChannelsFileDto
import data.dto.manifest.v2.UpdateManifestDto
import data.mapper.toDomain
import domain.models.channels.ChannelAccess
import domain.models.channels.ChannelsFile
import domain.models.manifest.v2.UpdateManifest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import newvision.CurrentChannelProvider
import newvision.UpdateChannel
import newvision.UpdateStateNewVision
import state.CheckUpdateState

class AppUpdateManagerImpl(
    client: KtorClient,
    private val channelAccessProvider: CurrentChannelProvider,
    private val host: String = HOST
): AppUpdateManager {
    private val clientInstance = client.baseInstance()

    override val checkUpdateState: Flow<CheckUpdateState>
        field = MutableStateFlow<CheckUpdateState>(CheckUpdateState.Idle)

    override val updateState: Flow<UpdateStateNewVision>
        field = MutableStateFlow<UpdateStateNewVision>(UpdateStateNewVision.Idle)

    override fun currentChannelAccess(): ChannelAccess {
        TODO("Not yet implemented")
    }

    override suspend fun checkUpdate(email: String, platform: String) {
        checkUpdateState.emit(CheckUpdateState.Loading)

        try {
            val channelsManifest = downloadChannelsManifest()
            val updateManifest = downloadUpdatesManifest()

            val currentAccessed = channelsManifest.users
                .getOrDefault(
                    key = email,
                    defaultValue = channelsManifest.default
                )
                .also { channelAccess ->
                    channelAccessProvider.saveCurrentAccessState(channelAccess)
                }

            // Получаем типизированный текущий канал пользователя с fallback на PROD.
            val typedCurrentChannel = UpdateChannel.entries.firstOrNull { channel ->
                channel.alias == currentAccessed.selected
            } ?: UpdateChannel.PROD

            val release = when (typedCurrentChannel) {
                UpdateChannel.PROD -> updateManifest.channels.prod
                UpdateChannel.DEV_CANARY -> updateManifest.channels.dev.canary
                UpdateChannel.DEV_RC -> updateManifest.channels.dev.rc
            }
        } catch (e: AppException.UpdateError) {
            checkUpdateState.emit(CheckUpdateState.Error(error = e))
        }
        finally {
            checkUpdateState.emit(CheckUpdateState.Idle)
        }

        TODO("Not yet implemented")
    }

    override suspend fun startUpdate() {
        TODO("Not yet implemented")
    }

    /** Файл каналов пользователей (channels.json). */
    private suspend fun downloadChannelsManifest(): ChannelsFile {
        return try {
            val response = clientInstance.get("https://$host/$CHANNELS_PATH")
            if (!response.status.isSuccess()) {
                error("Файл каналов недоступен: HTTP ${response.status.value}")
            }
            response.body<ChannelsFileDto>().toDomain()
        } catch (e: Exception) {
            println("[UpdateDownloader] Файл каналов недоступен: ${e.message}")
            TODO("Выбросить полную ошибку")
            throw AppException.UpdateError.CheckFailed()    // Преобразуем ошибку в проектную.
        }
    }

    private suspend fun downloadUpdatesManifest(): UpdateManifest {
        return try {
            val response = clientInstance.get("https://$host/$MANIFEST_PATH")
            if (!response.status.isSuccess()) {
                error("Манифест обновлений недоступен: HTTP ${response.status.value}")
            }
            response.body<UpdateManifestDto>().toDomain()
        } catch (e: Exception) {
            println("[UpdateDownloader] Манифест обновлений недоступен: ${e.message}")
            TODO("Выбросить полную ошибку")
            throw AppException.UpdateError.CheckFailed()    // Преобразуем ошибку в проектную.
        }
    }

    private companion object {
        const val HOST = "helpdesk.lpmti.ru"
        const val MANIFEST_PATH = "helpdesk-app/v2/update-manifest-v4.json"
        const val CHANNELS_PATH = "helpdesk-app/v2/users-settings/channels.json"
    }
}