package data.mapper

import data.dto.channels.ChannelAccessDto
import data.dto.channels.ChannelsFileDto
import domain.models.channels.ChannelAccess
import domain.models.channels.ChannelsFile

/**
 * Маппинг файла каналов пользователей на доменные модели.
 *
 * Ключи [usersDto] нормализуются в нижний регистр, чтобы поиск по email
 * не зависел от регистра в файле.
 */
internal fun ChannelsFileDto.toDomain(): ChannelsFile = ChannelsFile(
    default = this.defaultDto?.toDomain(),
    users = this.usersDto
        .mapKeys { it.key.lowercase() }
        .mapValues { (_, access) -> access.toDomain() },
)

private fun ChannelAccessDto.toDomain(): ChannelAccess = ChannelAccess(
    allowed = this.allowedDto.orEmpty(),
    selected = this.selectedDto,
)
