package domain

import domain.models.channels.ChannelAccess

/**
 * Выбор канала обновлений для пользователя.
 *
 * Приоритет: выбранный пользователем канал (если он входит в список
 * доступных) → первый доступный → последний известный канал из локального
 * хранилища → канал по умолчанию ([DEFAULT_CHANNEL]).
 *
 * Канал по умолчанию срабатывает, когда настройки доступа недоступны:
 * файл channels.json отсутствует на сервере или не содержит записи
 * о пользователе.
 */
object ChannelSelection {
    /** Канал, выбираемый когда настройки доступа определить не удалось. */
    const val DEFAULT_CHANNEL: String = UpdateChannel.DEV_CANARY

    /**
     * @param access доступ пользователя из файла каналов; null, если файл
     * недоступен или записи для пользователя нет.
     * @param lastChannel последний успешно использованный канал.
     * @return канал для запуска; всегда непустой.
     */
    fun select(access: ChannelAccess?, lastChannel: String?): String {
        val allowed = access?.allowed.orEmpty().filter { it.isNotBlank() }
        val selected = access?.selected?.trim()?.takeIf { it.isNotEmpty() }

        if (selected != null && (allowed.isEmpty() || selected in allowed)) return selected
        allowed.firstOrNull()?.let { return it }

        return lastChannel?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_CHANNEL
    }
}
