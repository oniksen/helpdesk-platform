package data.mapper

import data.dto.manifest.v2.UpdateManifestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

/**
 * Проверка соответствия DTO ответу сервера.
 *
 * Фикстура — дословный ответ `helpdesk-app/v2/update-manifest-v4.json` на момент
 * написания теста. Тест ломается, если сервер поменяет структуру манифеста или
 * если DTO перестанет совпадать с реальными ключами JSON.
 * */
class UpdateManifestV4WireFormatTest {

    @Test
    fun `maps the manifest published by the server`() {
        val manifest = Json.decodeFromString<UpdateManifestDto>(SERVER_MANIFEST).toDomain()

        assertEquals(4, manifest.schemaVersion, "Версия схемы манифеста сервера должна разбираться")

        val canary = manifest.channels.dev.canary
        val canaryWeb = assertNotNull(canary.web, "Опубликованный canary web-таргет должен разбираться")
        assertEquals("0.2.1005", canaryWeb.version, "Версия canary web-таргета разобрана неверно")
        assertEquals("2026-10-01T14:00:00", canaryWeb.date, "Дата canary web-таргета разобрана неверно")
        assertEquals(
            "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/canary/web/web-canary.zip",
            canaryWeb.link,
            "Ссылка canary web-таргета разобрана неверно",
        )
        assertEquals(16_408_543L, canaryWeb.size, "Размер canary web-таргета разобран неверно")

        // Сервер не публиковал эти сборки: хеш и размер пустые, поэтому таргеты null.
        assertNull(canary.macos, "Неопубликованный canary macos-таргет должен становиться null")
        assertNull(canary.windows, "Неопубликованный canary windows-таргет должен становиться null")
        assertNull(manifest.channels.dev.rc.web, "Неопубликованный rc web-таргет должен становиться null")
        assertNull(manifest.channels.prod.web, "Неопубликованный prod web-таргет должен становиться null")
        assertNull(manifest.channels.prod.macos, "Неопубликованный prod macos-таргет должен становиться null")
        assertNull(manifest.channels.prod.windows, "Неопубликованный prod windows-таргет должен становиться null")

        // Отклонение ещё не заполнено сервером: дата и причина приходят пустыми.
        val rejection = manifest.channels.dev.lastRejection
        assertEquals("0.0.0", rejection.lastVersion, "Версия отклонения разобрана неверно")
    }

    private companion object {
        val SERVER_MANIFEST = """
            {
              "schema_version": 4,
              "channels": {
                "prod": {
                  "macos": {
                    "version": "0.0.0",
                    "patch_note": [],
                    "tags": [],
                    "date": null,
                    "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/prod/macos/macos-latest.zip",
                    "hash": null,
                    "size": null
                  },
                  "windows": {
                    "version": "0.0.0",
                    "patch_note": [],
                    "tags": [],
                    "date": null,
                    "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/prod/windows/windows-latest.zip",
                    "hash": null,
                    "size": null
                  },
                  "web": {
                    "version": "0.0.0",
                    "patch_note": [],
                    "tags": [],
                    "date": null,
                    "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/prod/web/web-latest.zip",
                    "hash": null,
                    "size": null
                  }
                },
                "dev": {
                  "canary": {
                    "macos": {
                      "version": "0.0.0",
                      "patch_note": [],
                      "tags": [],
                      "date": null,
                      "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/canary/macos/macos-canary.zip",
                      "hash": null,
                      "size": null
                    },
                    "windows": {
                      "version": "0.0.0",
                      "patch_note": [],
                      "tags": [],
                      "date": null,
                      "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/canary/windows/windows-canary.zip",
                      "hash": null,
                      "size": null
                    },
                    "web": {
                      "version": "0.2.1005",
                      "patch_note": [],
                      "tags": [],
                      "date": "2026-10-01T14:00:00",
                      "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/canary/web/web-canary.zip",
                      "hash": "5541751553a3d58c3297b5cbeee1d32c6cc2053572cb8c2e30bd3c06c4f4f700",
                      "size": 16408543
                    }
                  },
                  "rc": {
                    "macos": {
                      "version": "0.0.0",
                      "patch_note": [],
                      "tags": [],
                      "date": null,
                      "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/rc/macos/macos-rc.zip",
                      "hash": null,
                      "size": null
                    },
                    "windows": {
                      "version": "0.0.0",
                      "patch_note": [],
                      "tags": [],
                      "date": null,
                      "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/rc/windows/windows-rc.zip",
                      "hash": null,
                      "size": null
                    },
                    "web": {
                      "version": "0.0.0",
                      "patch_note": [],
                      "tags": [],
                      "date": null,
                      "link": "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/dev/rc/web/web-rc.zip",
                      "hash": null,
                      "size": null
                    }
                  },
                  "last_rejection": {
                    "last_version": "0.0.0",
                    "reason": null,
                    "date": null
                  }
                }
              }
            }
        """.trimIndent()
    }
}
