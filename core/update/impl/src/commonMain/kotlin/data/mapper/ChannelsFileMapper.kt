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
    // Отсутствие default не должно ломать гейт: пустой доступ сохраняет
    // прежний fallback на последний известный канал / канал по умолчанию.
    default = this.defaultDto?.toDomain() ?: EMPTY_ACCESS,
    users = this.usersDto
        .mapKeys { it.key.lowercase() }
        .mapValues { (_, access) -> access.toDomain() },
)

/** Доступ по умолчанию, когда сервер не прислал секцию `default`. */
private val EMPTY_ACCESS = ChannelAccess(
    allowed = emptyList(),
    selected = null,
)

private fun ChannelAccessDto.toDomain(): ChannelAccess = ChannelAccess(
    allowed = this.allowedDto.orEmpty(),
    selected = this.selectedDto,
)
