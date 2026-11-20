package data.impl

import domain.models.channels.ChannelAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChannelAccessStoreImplTest {

    @Test
    fun `returns null before first save`() {
        assertNull(
            ChannelAccessStoreImpl().current(),
            "Новое хранилище не должно содержать доступ",
        )
    }

    @Test
    fun `returns last saved access`() {
        val store = ChannelAccessStoreImpl()
        val first = ChannelAccess(allowed = listOf("prod"), selected = "prod")
        val second = ChannelAccess(allowed = listOf("dev.canary"), selected = "dev.canary")

        store.save(first)
        assertEquals(first, store.current(), "Хранилище не вернуло сохранённый доступ")

        store.save(second)
        assertEquals(second, store.current(), "Хранилище не заменило доступ")
    }
}
