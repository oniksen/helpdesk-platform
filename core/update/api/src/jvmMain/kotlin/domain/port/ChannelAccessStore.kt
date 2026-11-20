package domain.port

import domain.models.channels.ChannelAccess

/**
 * Хранилище последнего известного доступа к каналам.
 *
 * Реализация по умолчанию — in-memory. Порт позволяет позже заменить её
 * на persisted-хранилище без изменения менеджера.
 */
interface ChannelAccessStore {
    fun save(access: ChannelAccess)
    fun current(): ChannelAccess?
}
