/**
 * Ключи localStorage, общие для гейта (webShell) и приложения (webApp).
 *
 * Гейт пишет сюда состояние, приложение читает его — ключ обязан быть
 * единственной константой, чтобы стороны не разъехались.
 */
object ChannelStorage {
    /** Последний успешно использованный канал обновлений. */
    const val LAST_CHANNEL_KEY = "helpdesk.gate.lastChannel"
}
