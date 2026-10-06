package domain.models.channels

/**
 * Доступ пользователя к каналам обновлений.
 *
 * @property allowed каналы, доступные пользователю (например, "dev.canary").
 * @property selected выбранный пользователем канал; null — не выбран.
 */
data class ChannelAccess(
    val allowed: List<String>,
    val selected: String?,
)

/**
 * Файл каналов пользователей (channels.json).
 *
 * @property default доступ для пользователей, которых нет в [users].
 * @property users доступ по email пользователя (ключи в нижнем регистре).
 */
data class ChannelsFile(
    val default: ChannelAccess?,
    val users: Map<String, ChannelAccess>,
) {
    /**
     * Доступ для пользователя; если записи по email нет — [default].
     */
    fun accessFor(email: String): ChannelAccess? = users[email.lowercase()] ?: default
}
