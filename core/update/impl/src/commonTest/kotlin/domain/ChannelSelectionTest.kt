package domain

import domain.models.channels.ChannelAccess
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelSelectionTest {

    @Test
    fun `selects channel by priority rules`() {
        CASES.forEach { (name, access, lastChannel, expected) ->
            assertEquals(
                expected,
                ChannelSelection.select(access, lastChannel),
                "Неверный канал для случая: $name",
            )
        }
    }

    private data class Case(
        val name: String,
        val access: ChannelAccess?,
        val lastChannel: String?,
        val expected: String?,
    )

    private companion object {
        val CASES = listOf(
            Case(
                name = "выбранный канал входит в доступные",
                access = ChannelAccess(allowed = listOf("dev.canary", "prod"), selected = "prod"),
                lastChannel = null,
                expected = "prod",
            ),
            Case(
                name = "выбранный канал вне списка доступных — берём первый доступный",
                access = ChannelAccess(allowed = listOf("dev.canary"), selected = "prod"),
                lastChannel = "dev.rc",
                expected = "dev.canary",
            ),
            Case(
                name = "канал не выбран — берём первый доступный",
                access = ChannelAccess(allowed = listOf("dev.rc", "dev.canary"), selected = null),
                lastChannel = null,
                expected = "dev.rc",
            ),
            Case(
                name = "список доступных пуст — берём выбранный канал",
                access = ChannelAccess(allowed = emptyList(), selected = "prod"),
                lastChannel = null,
                expected = "prod",
            ),
            Case(
                name = "выбранный канал пустой — берём первый доступный",
                access = ChannelAccess(allowed = listOf("dev.canary"), selected = "   "),
                lastChannel = null,
                expected = "dev.canary",
            ),
            Case(
                name = "пустые строки в доступных игнорируются",
                access = ChannelAccess(allowed = listOf("", "prod"), selected = null),
                lastChannel = null,
                expected = "prod",
            ),
            Case(
                name = "файла каналов нет — берём последний известный канал",
                access = null,
                lastChannel = "dev.canary",
                expected = "dev.canary",
            ),
            Case(
                name = "нет данных ни о канале, ни о прошлом запуске — канал по умолчанию",
                access = null,
                lastChannel = null,
                expected = "dev.canary",
            ),
            Case(
                name = "последний известный канал пустой — канал по умолчанию",
                access = null,
                lastChannel = "   ",
                expected = "dev.canary",
            ),
            Case(
                name = "записи о пользователе нет — последний известный канал важнее канала по умолчанию",
                access = null,
                lastChannel = "prod",
                expected = "prod",
            ),
        )
    }
}
