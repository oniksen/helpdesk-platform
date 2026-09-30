package data.mapper

import data.dto.manifest.ChannelsDto
import data.dto.manifest.CurrentDto
import data.dto.manifest.DevDto
import data.dto.manifest.LastRejectionDto
import data.dto.manifest.MacosDto
import data.dto.manifest.ProdDto
import data.dto.manifest.StableDto
import data.dto.manifest.UpdateManifestDto
import data.dto.manifest.WebDto
import data.dto.manifest.WindowsDto
import domain.models.manifest.Channels
import domain.models.manifest.Current
import domain.models.manifest.Dev
import domain.models.manifest.LastRejection
import domain.models.manifest.Macos
import domain.models.manifest.Prod
import domain.models.manifest.Stable
import domain.models.manifest.UpdateManifest
import domain.models.manifest.Web
import domain.models.manifest.Windows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdateManifestMapperTest {

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
                    current = validCurrent(patchNote = null, tags = null),
                    stable = validStable(patchNote = null, tags = null),
                ),
                prod = validProd(patchNote = null, tags = null),
            ),
        ).toDomain()

        OPTIONAL_LIST_NODES.forEach { (path, read) ->
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
    fun `treats a web target as unpublished when the hash is blank or the size is not positive`() {
        UNPUBLISHED_WEB_CASES.forEach { (reason, web) ->
            val manifest = validManifest(channels = validChannels(prod = validProd(web = web))).toDomain()

            assertNull(manifest.channels.prod.web, "Неопубликованный веб-таргет должен становиться null: $reason")
        }

        val published = validManifest().toDomain()
        assertEquals(
            Web(WEB_HASH, WEB_LINK, WEB_SIZE),
            published.channels.prod.web,
            "Опубликованный веб-таргет должен переноситься как есть",
        )
    }

    @Test
    fun `rejects a schema version below the minimum`() {
        assertManifestError("Отсутствует обязательное поле манифеста: schema_version") {
            validManifest(schemaVersion = null)
        }

        UNSUPPORTED_SCHEMA_VERSIONS.forEach { version ->
            assertManifestError("Некорректное значение поля манифеста: schema_version = $version") {
                validManifest(schemaVersion = version)
            }
        }

        assertEquals(
            SCHEMA_VERSION,
            validManifest().toDomain().schemaVersion,
            "Минимальная поддерживаемая версия схемы должна приниматься",
        )
    }

    private fun assertManifestError(expectedMessage: String, manifest: () -> UpdateManifestDto) {
        val error = assertFailsWith<IllegalStateException>("Ожидалась ошибка валидации манифеста: $expectedMessage") {
            manifest().toDomain()
        }

        assertEquals(expectedMessage, error.message, "Неверное сообщение об ошибке манифеста")
    }

    private data class MissingFieldCase(
        val manifest: UpdateManifestDto,
        val path: String,
    )

    private companion object {
        val UNSUPPORTED_SCHEMA_VERSIONS = listOf(-1, 0, 1, 2)

        /**
         * Патч-ноуты и теги объявлены опциональными, поэтому пустые списки проверяются
         * по каждому узлу отдельно: путь в сообщении ассерта показывает, какой именно
         * список не перенёсся.
         * */
        val OPTIONAL_LIST_NODES = listOf(
            "channels.dev.current.patch_note" to { m: UpdateManifest -> m.channels.dev.current.patchNote },
            "channels.dev.current.tags" to { m: UpdateManifest -> m.channels.dev.current.tags },
            "channels.dev.stable.patch_note" to { m: UpdateManifest -> m.channels.dev.stable.patchNote },
            "channels.dev.stable.tags" to { m: UpdateManifest -> m.channels.dev.stable.tags },
            "channels.prod.patch_note" to { m: UpdateManifest -> m.channels.prod.patchNote },
            "channels.prod.tags" to { m: UpdateManifest -> m.channels.prod.tags },
        )

        /**
         * Веб-таргет считается неопубликованным при пустом хеше или неположительном размере.
         * Отсутствующие (null) поля сюда не входят: они разбираются как обязательные
         * и дают другую ошибку.
         * */
        val UNPUBLISHED_WEB_CASES = listOf(
            "пустой хеш" to validWeb(hash = ""),
            "хеш из пробелов" to validWeb(hash = "   "),
            "нулевой размер" to validWeb(size = 0),
            "отрицательный размер" to validWeb(size = -1),
        )

        /**
         * Каждая строка соответствует одному вызову `required` в маппере: обнулено
         * ровно одно поле, поэтому путь в ошибке однозначен.
         * */
        val MISSING_FIELD_CASES = listOf(
            MissingFieldCase(validManifest(channels = null), "channels"),
            MissingFieldCase(validManifest(channels = validChannels(dev = null)), "channels.dev"),
            MissingFieldCase(devManifest(validDev(current = null)), "channels.dev.current"),
            MissingFieldCase(
                devManifest(validDev(current = validCurrent(date = null))),
                "channels.dev.current.date",
            ),
            MissingFieldCase(
                devManifest(validDev(current = validCurrent(lastVersion = null))),
                "channels.dev.current.last_version",
            ),
            MissingFieldCase(
                devManifest(validDev(current = validCurrent(macos = null))),
                "channels.dev.current.macos",
            ),
            MissingFieldCase(
                devManifest(validDev(current = validCurrent(macos = validMacos(link = null)))),
                "channels.dev.current.macos.link",
            ),
            MissingFieldCase(
                devManifest(validDev(current = validCurrent(patchNote = null, tags = null, web = null))),
                "channels.dev.current.web",
            ),
            MissingFieldCase(
                devManifest(validDev(current = validCurrent(windows = validWindows(size = null)))),
                "channels.dev.current.windows.size",
            ),
            MissingFieldCase(
                devManifest(validDev(lastRejection = null)),
                "channels.dev.last_rejection",
            ),
            MissingFieldCase(
                devManifest(validDev(lastRejection = validLastRejection(reason = null))),
                "channels.dev.last_rejection.reason",
            ),
            MissingFieldCase(devManifest(validDev(stable = null)), "channels.dev.stable"),
            MissingFieldCase(
                devManifest(validDev(stable = validStable(lastVersion = null))),
                "channels.dev.stable.last_version",
            ),
            MissingFieldCase(
                devManifest(validDev(stable = validStable(macos = validMacos(hash = null)))),
                "channels.dev.stable.macos.hash",
            ),
            MissingFieldCase(
                devManifest(validDev(stable = validStable(web = validWeb(size = null)))),
                "channels.dev.stable.web.size",
            ),
            MissingFieldCase(
                devManifest(validDev(stable = validStable(windows = null))),
                "channels.dev.stable.windows",
            ),
            MissingFieldCase(validManifest(channels = validChannels(prod = null)), "channels.prod"),
            MissingFieldCase(prodManifest(validProd(date = null)), "channels.prod.date"),
            MissingFieldCase(prodManifest(validProd(lastVersion = null)), "channels.prod.last_version"),
            MissingFieldCase(prodManifest(validProd(macos = null)), "channels.prod.macos"),
            MissingFieldCase(
                prodManifest(validProd(macos = validMacos(size = null))),
                "channels.prod.macos.size",
            ),
            MissingFieldCase(prodManifest(validProd(web = null)), "channels.prod.web"),
            MissingFieldCase(prodManifest(validProd(web = validWeb(hash = null))), "channels.prod.web.hash"),
            MissingFieldCase(prodManifest(validProd(windows = null)), "channels.prod.windows"),
            MissingFieldCase(
                prodManifest(validProd(windows = validWindows(link = null))),
                "channels.prod.windows.link",
            ),
        )

        fun expectedManifest() = UpdateManifest(
            schemaVersion = SCHEMA_VERSION,
            channels = Channels(
                dev = Dev(
                    current = expectedCurrent(CURRENT_VERSION, CURRENT_NOTE, CURRENT_TAGS),
                    lastRejection = LastRejection(DATE, REJECTED_VERSION, REJECTION_REASON),
                    stable = expectedStable(),
                ),
                prod = Prod(
                    date = DATE,
                    lastVersion = PROD_VERSION,
                    macos = expectedMacos(),
                    patchNote = PROD_NOTE,
                    tags = PROD_TAGS,
                    web = expectedWeb(),
                    windows = expectedWindows(),
                ),
            ),
        )

        fun expectedCurrent(version: String, patchNote: List<String>, tags: List<String>) = Current(
            date = DATE,
            lastVersion = version,
            macos = expectedMacos(),
            patchNote = patchNote,
            tags = tags,
            web = expectedWeb(),
            windows = expectedWindows(),
        )

        fun expectedStable() = Stable(
            date = DATE,
            lastVersion = STABLE_VERSION,
            macos = expectedMacos(),
            patchNote = STABLE_NOTE,
            tags = STABLE_TAGS,
            web = expectedWeb(),
            windows = expectedWindows(),
        )

        fun expectedMacos() = Macos(MACOS_HASH, MACOS_LINK, MACOS_SIZE)

        fun expectedWeb() = Web(WEB_HASH, WEB_LINK, WEB_SIZE)

        fun expectedWindows() = Windows(WINDOWS_HASH, WINDOWS_LINK, WINDOWS_SIZE)
    }
}

private const val SCHEMA_VERSION = 3
private const val DATE = "2026-01-10"
private const val CURRENT_VERSION = "0.1.1002"
private const val STABLE_VERSION = "0.1.1001"
private const val PROD_VERSION = "0.1.1000"
private const val REJECTED_VERSION = "0.1.999"
private const val REJECTION_REASON = "Сборка не прошла проверку"
private const val MACOS_HASH = "macos-hash"
private const val MACOS_LINK = "https://helpdesk.lpmti.ru/helpdesk-app/build.dmg"
private const val MACOS_SIZE = 2048
private const val WEB_HASH = "web-hash"
private const val WEB_LINK = "https://helpdesk.lpmti.ru/helpdesk-app/build.zip"
private const val WEB_SIZE = 1024
private const val WINDOWS_HASH = "windows-hash"
private const val WINDOWS_LINK = "https://helpdesk.lpmti.ru/helpdesk-app/build.exe"
private const val WINDOWS_SIZE = 512

private val CURRENT_NOTE = listOf("Текущий состав")
private val CURRENT_TAGS = listOf("current", "canary")
private val STABLE_NOTE = listOf("Стабильный состав")
private val STABLE_TAGS = listOf("stable")
private val PROD_NOTE = listOf("Продакшн состав")
private val PROD_TAGS = listOf("prod", "release")

private fun validManifest(
    channels: ChannelsDto? = validChannels(),
    schemaVersion: Int? = SCHEMA_VERSION,
) = UpdateManifestDto(channelsDto = channels, schemaVersionDto = schemaVersion)

private fun validChannels(
    dev: DevDto? = validDev(),
    prod: ProdDto? = validProd(),
) = ChannelsDto(devDto = dev, prodDto = prod)

private fun devManifest(dev: DevDto) = validManifest(channels = validChannels(dev = dev))

private fun prodManifest(prod: ProdDto) = validManifest(channels = validChannels(prod = prod))

private fun validDev(
    current: CurrentDto? = validCurrent(),
    lastRejection: LastRejectionDto? = validLastRejection(),
    stable: StableDto? = validStable(),
) = DevDto(currentDto = current, lastRejectionDto = lastRejection, stableDto = stable)

private fun validCurrent(
    date: String? = DATE,
    lastVersion: String? = CURRENT_VERSION,
    macos: MacosDto? = validMacos(),
    patchNote: List<String>? = CURRENT_NOTE,
    tags: List<String>? = CURRENT_TAGS,
    web: WebDto? = validWeb(),
    windows: WindowsDto? = validWindows(),
) = CurrentDto(
    dateDto = date,
    lastVersionDto = lastVersion,
    macosDto = macos,
    patchNoteDto = patchNote,
    tagsDto = tags,
    webDto = web,
    windowsDto = windows,
)

private fun validStable(
    date: String? = DATE,
    lastVersion: String? = STABLE_VERSION,
    macos: MacosDto? = validMacos(),
    patchNote: List<String>? = STABLE_NOTE,
    tags: List<String>? = STABLE_TAGS,
    web: WebDto? = validWeb(),
    windows: WindowsDto? = validWindows(),
) = StableDto(
    dateDto = date,
    lastVersionDto = lastVersion,
    macosDto = macos,
    patchNoteDto = patchNote,
    tagsDto = tags,
    webDto = web,
    windowsDto = windows,
)

private fun validProd(
    date: String? = DATE,
    lastVersion: String? = PROD_VERSION,
    macos: MacosDto? = validMacos(),
    patchNote: List<String>? = PROD_NOTE,
    tags: List<String>? = PROD_TAGS,
    web: WebDto? = validWeb(),
    windows: WindowsDto? = validWindows(),
) = ProdDto(
    dateDto = date,
    lastVersionDto = lastVersion,
    macosDto = macos,
    patchNoteDto = patchNote,
    tagsDto = tags,
    webDto = web,
    windowsDto = windows,
)

private fun validLastRejection(
    date: String? = DATE,
    lastVersion: String? = REJECTED_VERSION,
    reason: String? = REJECTION_REASON,
) = LastRejectionDto(dateDto = date, lastVersionDto = lastVersion, reasonDto = reason)

private fun validMacos(
    hash: String? = MACOS_HASH,
    link: String? = MACOS_LINK,
    size: Int? = MACOS_SIZE,
) = MacosDto(hashDto = hash, linkDto = link, sizeDto = size)

private fun validWeb(
    hash: String? = WEB_HASH,
    link: String? = WEB_LINK,
    size: Int? = WEB_SIZE,
) = WebDto(hashDto = hash, linkDto = link, sizeDto = size)

private fun validWindows(
    hash: String? = WINDOWS_HASH,
    link: String? = WINDOWS_LINK,
    size: Int? = WINDOWS_SIZE,
) = WindowsDto(hashDto = hash, linkDto = link, sizeDto = size)
