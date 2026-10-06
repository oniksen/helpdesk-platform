package data.dto.channels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO файла каналов пользователей (channels.json).
 *
 * Файл отвечает на два вопроса: каким каналам имеет доступ пользователь
 * и какой канал у него сейчас выбран. Ключи [usersDto] — email
 * пользователей (регистр не важен).
 */
@Serializable
data class ChannelsFileDto(
    @SerialName("default") val defaultDto: ChannelAccessDto? = null,
    @SerialName("users") val usersDto: Map<String, ChannelAccessDto> = emptyMap(),
)

/**
 * Доступ конкретного пользователя к каналам обновлений.
 */
@Serializable
data class ChannelAccessDto(
    @SerialName("allowed") val allowedDto: List<String>? = null,
    @SerialName("selected") val selectedDto: String? = null,
)
