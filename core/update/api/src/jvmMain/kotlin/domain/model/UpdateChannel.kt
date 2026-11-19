package domain.model

import domain.UpdateChannel as ChannelAliases

/**
 * Типизированный канал обновлений для JVM.
 *
 * Алиасы берутся из общего [ChannelAliases], чтобы не дублировать строки.
 */
enum class UpdateChannel(val alias: String) {
    PROD(ChannelAliases.PROD),
    DEV_CANARY(ChannelAliases.DEV_CANARY),
    DEV_RC(ChannelAliases.DEV_RC);

    companion object {
        /** Канал по серверному алиасу; неизвестный или пустой алиас → null. */
        fun fromAlias(alias: String?): UpdateChannel? =
            entries.firstOrNull { it.alias == alias?.trim() }
    }
}
