package data.mapper

import data.dto.channels.ChannelAccessDto
import data.dto.channels.ChannelsFileDto
import data.mapper.toDomain
import domain.models.channels.ChannelAccess
import kotlin.test.Test
import kotlin.test.assertEquals

class ChannelsFileMapperTest {

    @Test
    fun `maps users with case-insensitive email keys`() {
        val dto = ChannelsFileDto(
            defaultDto = ChannelAccessDto(
                allowedDto = listOf("dev.canary"),
                selectedDto = "dev.canary",
            ),
            usersDto = mapOf(
                "Ivanov@Corp.RU" to ChannelAccessDto(
                    allowedDto = listOf("dev.canary", "prod"),
                    selectedDto = "prod",
                ),
            ),
        )

        val file = dto.toDomain()

        assertEquals(
            "prod",
            file.accessFor("ivanov@corp.ru")?.selected,
            "Поиск пользователя должен игнорировать регистр email",
        )
        assertEquals(
            "prod",
            file.accessFor("IVANOV@corp.ru")?.selected,
            "Поиск пользователя должен игнорировать регистр email",
        )
        assertEquals(
            "dev.canary",
            file.accessFor("unknown@corp.ru")?.selected,
            "Неизвестный пользователь должен получать доступ по умолчанию",
        )
    }

    @Test
    fun `maps missing default and users to empty access`() {
        val file = ChannelsFileDto().toDomain()
        val emptyAccess = ChannelAccess(allowed = emptyList(), selected = null)

        assertEquals(
            emptyAccess,
            file.default,
            "Отсутствующий default должен маппиться в пустой доступ",
        )
        assertEquals(emptyMap(), file.users, "Отсутствующие users должны маппиться в пустую карту")
        assertEquals(
            emptyAccess,
            file.accessFor("anyone@corp.ru"),
            "Без default у неизвестного пользователя должен быть пустой доступ",
        )
    }
}
