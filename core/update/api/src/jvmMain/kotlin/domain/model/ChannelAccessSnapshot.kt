package domain.model

/**
 * Полное состояние каналов для UI и диагностики.
 *
 * @property allowed Известные приложению каналы, доступные пользователю,
 * в порядке файла каналов.
 * @property selected Канал, выбранный пользователем (если известен приложению).
 * @property current Эффективный канал, по которому шла проверка.
 * @property source Откуда взяты данные о каналах.
 */
data class ChannelAccessSnapshot(
    val allowed: List<UpdateChannel> = emptyList(),
    val selected: UpdateChannel? = null,
    val current: UpdateChannel = UpdateChannel.PROD,
    val source: ChannelAccessSource = ChannelAccessSource.FALLBACK,
)

/** Источник данных о каналах. */
enum class ChannelAccessSource {
    /** channels.json успешно прочитан. */
    SERVER,

    /** channels.json не читался (email == null) или недоступен. */
    FALLBACK,
}
