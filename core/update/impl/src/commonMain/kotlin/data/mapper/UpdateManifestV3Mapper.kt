package data.mapper

import data.dto.manifest.v1.ChannelsDto
import data.dto.manifest.v1.CurrentDto
import data.dto.manifest.v1.DevDto
import data.dto.manifest.v1.LastRejectionDto
import data.dto.manifest.v1.MacosDto
import data.dto.manifest.v1.ProdDto
import data.dto.manifest.v1.StableDto
import data.dto.manifest.v1.UpdateManifestDto
import data.dto.manifest.v1.WebDto
import data.dto.manifest.v1.WindowsDto
import domain.ManifestSupported
import domain.models.manifest.v1.Channels
import domain.models.manifest.v1.Current
import domain.models.manifest.v1.Dev
import domain.models.manifest.v1.LastRejection
import domain.models.manifest.v1.Macos
import domain.models.manifest.v1.Prod
import domain.models.manifest.v1.Stable
import domain.models.manifest.v1.UpdateManifest
import domain.models.manifest.v1.Web
import domain.models.manifest.v1.Windows

private const val MISSING_FIELD_ERROR = "Отсутствует обязательное поле манифеста"
private const val INVALID_FIELD_ERROR = "Некорректное значение поля манифеста"
private val SCHEMA_VERSION = ManifestSupported.getManifestVersion("v1")
private const val MAX_UNPUBLISHED_SIZE = 0

/**
 * Маппинг манифеста обновлений на доменные модели.
 *
 * Маппинг строгий: отсутствие любого обязательного поля приводит к ошибке.
 * Исключения — патч-ноуты и теги (для них отсутствие значения равносильно
 * пустому списку), версия схемы, которая должна быть положительным числом,
 * и веб-таргет, который считается неопубликованным при пустом хеше или
 * неположительном размере.
 * */
internal fun UpdateManifestDto.toDomain(): UpdateManifest = UpdateManifest(
    channels = this.channelsDto.required("channels").toDomain(),
    schemaVersion = this.schemaVersionDto.schemaVersion("schema_version"),
)

private fun ChannelsDto.toDomain(): Channels = Channels(
    dev = this.devDto.required("channels.dev").toDomain(),
    prod = this.prodDto.required("channels.prod").toDomain(),
)

private fun DevDto.toDomain(): Dev = Dev(
    current = this.currentDto.required("channels.dev.current").toDomain(),
    lastRejection = this.lastRejectionDto.required("channels.dev.last_rejection").toDomain(),
    stable = this.stableDto.required("channels.dev.stable").toDomain(),
)

private fun CurrentDto.toDomain(): Current {
    val path = "channels.dev.current"
    return Current(
        date = this.dateDto.required("$path.date"),
        lastVersion = this.lastVersionDto.required("$path.last_version"),
        macos = this.macosDto.required("$path.macos").toDomain("$path.macos"),
        patchNote = this.patchNoteDto.orEmptyList(),
        tags = this.tagsDto.orEmptyList(),
        web = this.webDto.required("$path.web").toDomain("$path.web"),
        windows = this.windowsDto.required("$path.windows").toDomain("$path.windows"),
    )
}

private fun LastRejectionDto.toDomain(): LastRejection {
    val path = "channels.dev.last_rejection"
    return LastRejection(
        date = this.dateDto.required("$path.date"),
        lastVersion = this.lastVersionDto.required("$path.last_version"),
        reason = this.reasonDto.required("$path.reason"),
    )
}

private fun StableDto.toDomain(): Stable {
    val path = "channels.dev.stable"
    return Stable(
        date = this.dateDto.required("$path.date"),
        lastVersion = this.lastVersionDto.required("$path.last_version"),
        macos = this.macosDto.required("$path.macos").toDomain("$path.macos"),
        patchNote = this.patchNoteDto.orEmptyList(),
        tags = this.tagsDto.orEmptyList(),
        web = this.webDto.required("$path.web").toDomain("$path.web"),
        windows = this.windowsDto.required("$path.windows").toDomain("$path.windows"),
    )
}

private fun ProdDto.toDomain(): Prod {
    val path = "channels.prod"
    return Prod(
        date = this.dateDto.required("$path.date"),
        lastVersion = this.lastVersionDto.required("$path.last_version"),
        macos = this.macosDto.required("$path.macos").toDomain("$path.macos"),
        patchNote = this.patchNoteDto.orEmptyList(),
        tags = this.tagsDto.orEmptyList(),
        web = this.webDto.required("$path.web").toDomain("$path.web"),
        windows = this.windowsDto.required("$path.windows").toDomain("$path.windows"),
    )
}

private fun MacosDto.toDomain(path: String): Macos = Macos(
    hash = this.hashDto.required("$path.hash"),
    link = this.linkDto.required("$path.link"),
    size = this.sizeDto.required("$path.size"),
)

private fun WebDto.toDomain(path: String): Web? {
    val link = this.linkDto.required("$path.link")
    val hash = this.hashDto.required("$path.hash")
    val size = this.sizeDto.required("$path.size")

    if (hash.isBlank() || size <= MAX_UNPUBLISHED_SIZE) return null

    return Web(
        hash = hash,
        link = link,
        size = size,
    )
}

private fun WindowsDto.toDomain(path: String): Windows = Windows(
    hash = this.hashDto.required("$path.hash"),
    link = this.linkDto.required("$path.link"),
    size = this.sizeDto.required("$path.size"),
)

/**
 * Обязательное поле манифеста: [null] означает, что сервер его не прислал.
 * */
private fun <T> T?.required(path: String): T = this ?: error("$MISSING_FIELD_ERROR: $path")

/**
 * Версия схемы манифеста: должна быть положительным целым числом.
 * */
private fun Int?.schemaVersion(path: String): Int {
    val version = this ?: error("$MISSING_FIELD_ERROR: $path")
    if (version < SCHEMA_VERSION) error("$INVALID_FIELD_ERROR: $path = $version")
    return version
}

/**
 * Патч-ноуты и теги опциональны: отсутствие значения равносильно пустому списку.
 * */
private fun List<String>?.orEmptyList(): List<String> = this ?: emptyList()
