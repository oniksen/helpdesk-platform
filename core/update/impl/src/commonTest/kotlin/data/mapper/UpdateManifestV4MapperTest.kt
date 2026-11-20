package data.mapper

import data.dto.manifest.v2.ChannelsDto
import data.dto.manifest.v2.DevDto
import data.dto.manifest.v2.LastRejectionDto
import data.dto.manifest.v2.ReleaseDto
import data.dto.manifest.v2.TargetDataDto
import data.dto.manifest.v2.UpdateManifestDto
import domain.models.manifest.v2.Channels
import domain.models.manifest.v2.Dev
import domain.models.manifest.v2.LastRejection
import domain.models.manifest.v2.Release
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UpdateManifestV4MapperTest {

    @Test
    fun `maps a complete manifest to domain models`() {
        val manifest = validManifest().toDomain()

        assertEquals(expectedManifest(), manifest, "Манифест перенесён на доменные модели с ошибками: $manifest")
    }

    @Test
    fun `treats absent patch notes and tags as empty lists`() {
        val manifest = validManifest(
            channels = validChannels(
                dev = validDev(
                    canary = validCanary(macos = validMacosTarget(patchNote = null, tags = null)),
                    rc = validRc(macos = validMacosTarget(patchNote = null, tags = null)),
                ),
                prod = validProd(web = validWebTarget(patchNote = null, tags = null)),
            ),
        ).toDomain()

        OPTIONAL_LIST_TARGETS.forEach { (path, read) ->
            assertEquals(emptyList(), read(manifest), "Отсутствующий список должен становиться пустым: $path")
        }
    }

    @Test
    fun `rejects a manifest with a missing required field`() {
        MISSING_FIELD_CASES.forEach { (manifest, path) ->
            val error = assertFailsWith<IllegalStateException>(
                "Обязательное поле '$path' должно приводить к ошибке",
            ) {
                manifest.toDomain()
            }

            assertEquals(
                "Отсутствует обязательное поле манифеста: $path",
                error.message,
                "Неверный путь поля в сообщении об ошибке для '$path'",
            )
        }
    }

    @Test
    fun `treats a target as unpublished when the hash is blank or the size is not positive`() {
        UNPUBLISHED_TARGET_CASES.forEach { (reason, target) ->
            val manifest = validManifest(
                channels = validChannels(dev = validDev(canary = validCanary(web = target))),
            ).toDomain()

            assertNull(manifest.channels.dev.canary.web, "Неопубликованный таргет должен становиться null: $reason")
        }

        val published = validManifest().toDomain()
        assertEquals(
            expectedWebTarget(CANARY_VERSION),
            published.channels.dev.canary.web,
            "Опубликованный таргет должен переноситься на доменную модель",
        )
    }

    @Test
    fun `skips field validation of an unpublished target`() {
        // Неопубликованный таргет сервер описывает пустыми полями, поэтому
        // отсутствие версии, даты и ссылки не должно ронять разбор манифеста.
        val manifest = validManifest(
            channels = validChannels(
                dev = validDev(
                    canary = validCanary(
                        web = validWebTarget(version = null, date = null, link = null, hash = null),
                    ),
                ),
            ),
        ).toDomain()

        assertNull(manifest.channels.dev.canary.web, "Неопубликованный таргет должен становиться null")
        assertNotNull(manifest.channels.dev.rc, "Остальные ветки обновлений должны переноситься как обычно")
    }

    @Test
    fun `treats an unfilled rejection as valid`() {
        // Сервер не заполняет дату и причину, пока отклонение не случилось.
        val manifest = validManifest(
            channels = validChannels(dev = validDev(lastRejection = validLastRejection(date = null, reason = null))),
        ).toDomain()

        assertEquals(
            LastRejection(date = null, lastVersion = REJECTED_VERSION, reason = null),
            manifest.channels.dev.lastRejection,
            "Незаполненное отклонение должно переноситься с пустыми датой и причиной",
        )
    }

    @Test
    fun `rejects a schema version below the minimum`() {
        UNSUPPORTED_SCHEMA_VERSIONS.forEach { version ->
            val error = assertFailsWith<IllegalStateException>(
                "Версия схемы '$version' должна приводить к ошибке",
            ) {
                validManifest(schemaVersion = version).toDomain()
            }

            assertEquals(
                "Некорректное значение поля манифеста: schema_version = $version",
                error.message,
                "Неверное сообщение об ошибке для версии схемы '$version'",
            )
        }

        val missing = assertFailsWith<IllegalStateException>(
            "Отсутствие версии схемы должно приводить к ошибке",
        ) {
            validManifest(schemaVersion = null).toDomain()
        }
        assertEquals(
            "Отсутствует обязательное поле манифеста: schema_version",
            missing.message,
            "Неверное сообщение об ошибке для отсутствующей версии схемы",
        )

        assertEquals(
            SCHEMA_VERSION,
            validManifest(schemaVersion = SCHEMA_VERSION).toDomain().schemaVersion,
            "Минимальная поддерживаемая версия схемы должна приниматься",
        )
    }

    private data class MissingFieldCase(
        val manifest: UpdateManifestDto,
        val path: String,
    )

    private companion object {
        val UNSUPPORTED_SCHEMA_VERSIONS = listOf(-1, 0, 1, 2, 3)

        /**
         * Таргет считается неопубликованным при пустом хеше или неположительном
         * размере. Отсутствующие (null) поля тоже означают неопубликованный
         * таргет: сервер именно так их и описывает.
         * */
        val UNPUBLISHED_TARGET_CASES = listOf(
            "хеш отсутствует" to validWebTarget(hash = null),
            "пустой хеш" to validWebTarget(hash = ""),
            "хеш из пробелов" to validWebTarget(hash = "   "),
            "размер отсутствует" to validWebTarget(size = null),
            "нулевой размер" to validWebTarget(size = 0L),
            "отрицательный размер" to validWebTarget(size = -1L),
        )

        /**
         * Патч-ноуты и теги объявлены опциональными, поэтому пустые списки проверяются
         * по каждому таргету отдельно: путь в сообщении ассерта показывает, какой именно
         * список не перенёсся. [null] вместо списка означает, что таргет не опубликован.
         * */
        val OPTIONAL_LIST_TARGETS = listOf(
            "channels.dev.canary.macos.patch_note" to { m: UpdateManifest -> m.channels.dev.canary.macos?.patchNote },
            "channels.dev.canary.macos.tags" to { m: UpdateManifest -> m.channels.dev.canary.macos?.tags },
            "channels.dev.rc.macos.patch_note" to { m: UpdateManifest -> m.channels.dev.rc.macos?.patchNote },
            "channels.dev.rc.macos.tags" to { m: UpdateManifest -> m.channels.dev.rc.macos?.tags },
            "channels.prod.web.patch_note" to { m: UpdateManifest -> m.channels.prod.web?.patchNote },
            "channels.prod.web.tags" to { m: UpdateManifest -> m.channels.prod.web?.tags },
        )

        /**
         * Каждая строка соответствует одному вызову `required` в маппере: обнулено
         * ровно одно поле, поэтому путь в ошибке однозначен. Поля таргетов проверяются
         * на `dev.canary.macos`, оставшемся опубликованным, — иначе вместо ошибки о
         * пропущенном поле вернулся бы [null] неопубликованного таргета.
         * */
        val MISSING_FIELD_CASES = listOf(
            MissingFieldCase(validManifest(channels = null), "channels"),
            MissingFieldCase(validManifest(channels = validChannels(dev = null)), "channels.dev"),
            MissingFieldCase(validManifest(channels = validChannels(prod = null)), "channels.prod"),
            MissingFieldCase(devManifest(validDev(canary = null)), "channels.dev.canary"),
            MissingFieldCase(devManifest(validDev(rc = null)), "channels.dev.rc"),
            MissingFieldCase(devManifest(validDev(lastRejection = null)), "channels.dev.last_rejection"),
            MissingFieldCase(
                devManifest(validDev(lastRejection = validLastRejection(lastVersion = null))),
                "channels.dev.last_rejection.last_version",
            ),
            MissingFieldCase(canaryManifest(validCanary(macos = null)), "channels.dev.canary.macos"),
            MissingFieldCase(canaryManifest(validCanary(web = null)), "channels.dev.canary.web"),
            MissingFieldCase(canaryManifest(validCanary(windows = null)), "channels.dev.canary.windows"),
            MissingFieldCase(
                canaryManifest(validCanary(macos = validMacosTarget(version = null))),
                "channels.dev.canary.macos.version",
            ),
            MissingFieldCase(
                canaryManifest(validCanary(macos = validMacosTarget(date = null))),
                "channels.dev.canary.macos.date",
            ),
            MissingFieldCase(
                canaryManifest(validCanary(macos = validMacosTarget(link = null))),
                "channels.dev.canary.macos.link",
            ),
            MissingFieldCase(prodManifest(validProd(macos = null)), "channels.prod.macos"),
            MissingFieldCase(prodManifest(validProd(web = null)), "channels.prod.web"),
            MissingFieldCase(prodManifest(validProd(windows = null)), "channels.prod.windows"),
        )

        fun expectedManifest() = UpdateManifest(
            schemaVersion = SCHEMA_VERSION,
            channels = Channels(
                dev = Dev(
                    canary = Release(
                        macos = expectedTarget(PROD_VERSION, MACOS_NOTE, MACOS_TAGS, MACOS_LINK, MACOS_HASH, MACOS_SIZE),
                        web = expectedWebTarget(CANARY_VERSION),
                        windows = expectedTarget(
                            PROD_VERSION,
                            WINDOWS_NOTE,
                            WINDOWS_TAGS,
                            WINDOWS_LINK,
                            WINDOWS_HASH,
                            WINDOWS_SIZE,
                        ),
                    ),
                    rc = Release(
                        macos = expectedTarget(RC_VERSION, MACOS_NOTE, MACOS_TAGS, MACOS_LINK, MACOS_HASH, MACOS_SIZE),
                        web = expectedWebTarget(RC_VERSION),
                        windows = expectedTarget(
                            RC_VERSION,
                            WINDOWS_NOTE,
                            WINDOWS_TAGS,
                            WINDOWS_LINK,
                            WINDOWS_HASH,
                            WINDOWS_SIZE,
                        ),
                    ),
                    lastRejection = LastRejection(DATE, REJECTED_VERSION, REJECTION_REASON),
                ),
                prod = Release(
                    macos = expectedTarget(PROD_VERSION, MACOS_NOTE, MACOS_TAGS, MACOS_LINK, MACOS_HASH, MACOS_SIZE),
                    web = expectedWebTarget(PROD_VERSION),
                    windows = expectedTarget(
                        PROD_VERSION,
                        WINDOWS_NOTE,
                        WINDOWS_TAGS,
                        WINDOWS_LINK,
                        WINDOWS_HASH,
                        WINDOWS_SIZE,
                    ),
                ),
            ),
        )

        fun expectedWebTarget(version: String) = expectedTarget(
            version = version,
            patchNote = WEB_NOTE,
            tags = WEB_TAGS,
            link = WEB_LINK,
            hash = WEB_HASH,
            size = WEB_SIZE,
        )

        fun expectedTarget(
            version: String,
            patchNote: List<String>,
            tags: List<String>,
            link: String,
            hash: String,
            size: Long,
        ) = TargetData(
            version = version,
            patchNote = patchNote,
            tags = tags,
            date = DATE,
            link = link,
            hash = hash,
            size = size,
        )
    }
}

private const val SCHEMA_VERSION = 4
private const val DATE = "2026-10-01T14:00:00"
private const val PROD_VERSION = "0.2.1005"
private const val CANARY_VERSION = "0.2.1006"
private const val RC_VERSION = "0.2.1007"
private const val REJECTED_VERSION = "0.1.999"
private const val REJECTION_REASON = "Сборка не прошла проверку"
private const val MACOS_HASH = "macos-hash"
private const val MACOS_LINK = "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/prod/macos/macos-latest.zip"
private const val MACOS_SIZE = 156_745_598L
private const val WEB_HASH = "web-hash"
private const val WEB_LINK = "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/prod/web/web-latest.zip"
private const val WEB_SIZE = 16_408_543L
private const val WINDOWS_HASH = "windows-hash"
private const val WINDOWS_LINK = "https://helpdesk.lpmti.ru/helpdesk-app/channels-v2/prod/windows/windows-latest.zip"
private const val WINDOWS_SIZE = 149_511_755L

private val MACOS_NOTE = listOf("Сборка для macOS")
private val MACOS_TAGS = listOf("macos", "release")
private val WEB_NOTE = listOf("Сборка для Web")
private val WEB_TAGS = listOf("web", "canary")
private val WINDOWS_NOTE = listOf("Сборка для Windows")
private val WINDOWS_TAGS = listOf("windows", "release")

private fun validManifest(
    channels: ChannelsDto? = validChannels(),
    schemaVersion: Int? = SCHEMA_VERSION,
) = UpdateManifestDto(channelsDto = channels, schemaVersionDto = schemaVersion)

private fun validChannels(
    dev: DevDto? = validDev(),
    prod: ReleaseDto? = validProd(),
) = ChannelsDto(devDto = dev, prodDto = prod)

private fun devManifest(dev: DevDto) = validManifest(channels = validChannels(dev = dev))

private fun prodManifest(prod: ReleaseDto) = validManifest(channels = validChannels(prod = prod))

private fun canaryManifest(canary: ReleaseDto) = devManifest(validDev(canary = canary))

private fun validDev(
    canary: ReleaseDto? = validCanary(),
    rc: ReleaseDto? = validRc(),
    lastRejection: LastRejectionDto? = validLastRejection(),
) = DevDto(canaryDto = canary, rcDto = rc, lastRejectionDto = lastRejection)

private fun validCanary(
    macos: TargetDataDto? = validMacosTarget(),
    web: TargetDataDto? = validWebTarget(version = CANARY_VERSION),
    windows: TargetDataDto? = validWindowsTarget(),
) = ReleaseDto(macosDto = macos, webDto = web, windowsDto = windows)

private fun validRc(
    macos: TargetDataDto? = validMacosTarget(version = RC_VERSION),
    web: TargetDataDto? = validWebTarget(version = RC_VERSION),
    windows: TargetDataDto? = validWindowsTarget(version = RC_VERSION),
) = ReleaseDto(macosDto = macos, webDto = web, windowsDto = windows)

private fun validProd(
    macos: TargetDataDto? = validMacosTarget(),
    web: TargetDataDto? = validWebTarget(),
    windows: TargetDataDto? = validWindowsTarget(),
) = ReleaseDto(macosDto = macos, webDto = web, windowsDto = windows)

private fun validLastRejection(
    date: String? = DATE,
    lastVersion: String? = REJECTED_VERSION,
    reason: String? = REJECTION_REASON,
) = LastRejectionDto(dateDto = date, lastVersionDto = lastVersion, reasonDto = reason)

private fun validMacosTarget(
    version: String? = PROD_VERSION,
    date: String? = DATE,
    patchNote: List<String>? = MACOS_NOTE,
    tags: List<String>? = MACOS_TAGS,
    link: String? = MACOS_LINK,
    hash: String? = MACOS_HASH,
    size: Long? = MACOS_SIZE,
) = TargetDataDto(
    dateDto = date,
    versionDto = version,
    patchNoteDto = patchNote,
    tagsDto = tags,
    linkDto = link,
    hashDto = hash,
    sizeDto = size,
)

private fun validWebTarget(
    version: String? = PROD_VERSION,
    date: String? = DATE,
    patchNote: List<String>? = WEB_NOTE,
    tags: List<String>? = WEB_TAGS,
    link: String? = WEB_LINK,
    hash: String? = WEB_HASH,
    size: Long? = WEB_SIZE,
) = TargetDataDto(
    dateDto = date,
    versionDto = version,
    patchNoteDto = patchNote,
    tagsDto = tags,
    linkDto = link,
    hashDto = hash,
    sizeDto = size,
)

private fun validWindowsTarget(
    version: String? = PROD_VERSION,
    date: String? = DATE,
    patchNote: List<String>? = WINDOWS_NOTE,
    tags: List<String>? = WINDOWS_TAGS,
    link: String? = WINDOWS_LINK,
    hash: String? = WINDOWS_HASH,
    size: Long? = WINDOWS_SIZE,
) = TargetDataDto(
    dateDto = date,
    versionDto = version,
    patchNoteDto = patchNote,
    tagsDto = tags,
    linkDto = link,
    hashDto = hash,
    sizeDto = size,
)
