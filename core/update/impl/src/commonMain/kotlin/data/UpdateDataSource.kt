package data

import KtorClient
import data.dto.channels.ChannelsFileDto
import data.dto.manifest.v2.UpdateManifestDto
import data.mapper.toDomain
import domain.models.channels.ChannelsFile
import domain.models.manifest.v2.UpdateManifest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Загрузка серверных данных обновлений: манифеста и файла каналов.
 *
 * Ответы с не-2xx статусом и ошибки разбора JSON пробрасываются
 * вызывающей стороне — гейт решает, что делать (fallback или ошибка).
 */
class UpdateDataSource(client: KtorClient) {
    private val http = client.baseInstance()

    /** Манифест обновлений (update-manifest-v4.json). */
    suspend fun fetchManifest(): UpdateManifest {
        val response = http.get("https://$HOST/$MANIFEST_PATH")
        if (!response.status.isSuccess()) {
            error("Манифест обновлений недоступен: HTTP ${response.status.value}")
        }
        return response.body<UpdateManifestDto>().toDomain()
    }

    /** Файл каналов пользователей (channels.json). */
    suspend fun fetchChannelsFile(): ChannelsFile {
        val response = http.get("https://$HOST/$CHANNELS_PATH")
        if (!response.status.isSuccess()) {
            error("Файл каналов недоступен: HTTP ${response.status.value}")
        }
        return response.body<ChannelsFileDto>().toDomain()
    }

    private companion object {
        const val HOST = "helpdesk.lpmti.ru"
        const val MANIFEST_PATH = "helpdesk-app/v2/update-manifest-v4.json"
        const val CHANNELS_PATH = "channels.json"
    }
}
