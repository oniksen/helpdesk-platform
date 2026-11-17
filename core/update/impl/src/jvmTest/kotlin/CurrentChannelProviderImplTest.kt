import domain.UpdateChannel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Текущий канал JVM-приложения задан константой: dev-каналы относятся к web-сборкам.
 */
class CurrentChannelProviderImplTest {

    @Test
    fun `reports prod as the current jvm channel`() {
        assertEquals(
            UpdateChannel.PROD,
            CurrentChannelProviderImpl().provide(),
            "JVM-приложение должно выбирать prod-канал",
        )
    }
}
