package org.lpmti.helpdeskplatform.gate

import ChannelStorage
import kotlinx.browser.localStorage

/**
 * Локальное состояние гейта между запусками.
 *
 * @property lastUrl последний адрес сборки, на который был выполнен редирект.
 * Используется как fallback, когда манифест обновлений недоступен (офлайн).
 * @property lastChannel последний успешно использованный канал.
 * Используется, когда файл каналов недоступен.
 */
internal class GateStorage {
    var lastUrl: String?
        get() = localStorage.getItem(KEY_LAST_URL)
        set(value) = setItem(KEY_LAST_URL, value)

    var lastChannel: String?
        get() = localStorage.getItem(ChannelStorage.LAST_CHANNEL_KEY)
        set(value) = setItem(ChannelStorage.LAST_CHANNEL_KEY, value)

    private fun setItem(key: String, value: String?) {
        if (value == null) {
            localStorage.removeItem(key)
        } else {
            localStorage.setItem(key, value)
        }
    }

    private companion object {
        const val KEY_LAST_URL = "helpdesk.gate.lastUrl"
    }
}
