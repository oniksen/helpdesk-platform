package domain

import domain.model.UpdateChannel as TypedChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Типизированные каналы JVM: маппинг серверных алиасов.
 */
class UpdateChannelTest {

    @Test
    fun `maps known aliases to typed channels`() {
        val cases = mapOf(
            "prod" to TypedChannel.PROD,
            "dev.canary" to TypedChannel.DEV_CANARY,
            "dev.rc" to TypedChannel.DEV_RC,
            " dev.rc " to TypedChannel.DEV_RC,
        )

        cases.forEach { (alias, expected) ->
            assertEquals(
                expected,
                TypedChannel.fromAlias(alias),
                "Неверный канал для алиаса '$alias'",
            )
        }
    }

    @Test
    fun `returns null for unknown or blank aliases`() {
        listOf(null, "", "   ", "dev.unknown").forEach { alias ->
            assertNull(
                TypedChannel.fromAlias(alias),
                "Неизвестный алиас '$alias' не должен маппиться",
            )
        }
    }
}
