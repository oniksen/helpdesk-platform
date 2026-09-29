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

private const val MISSING_FIELD_ERROR = "Отсутствует обязательное поле манифеста"
private const val INVALID_FIELD_ERROR = "Некорректное значение поля манифеста"
private const val MIN_SCHEMA_VERSION = 3

/**
 * Маппинг манифеста обновлений на доменные модели.
 *
 * Маппинг строгий: отсутствие любого обязательного поля приводит к ошибке.
 * Исключения — патч-ноуты и теги (для них отсутствие значения равносильно
 * пустому списку) и версия схемы, которая должна быть положительным числом.
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
        needRestart = this.needRestartDto.required("$path.need_restart"),
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
        needRestart = this.needRestartDto.required("$path.need_restart"),
        patchNote = this.patchNoteDto.orEmptyList(),
        tags = this.tagsDto.orEmptyList(),
        windows = this.windowsDto.required("$path.windows").toDomain("$path.windows"),
    )
}

private fun ProdDto.toDomain(): Prod {
    val path = "channels.prod"
    return Prod(
        date = this.dateDto.required("$path.date"),
        lastVersion = this.lastVersionDto.required("$path.last_version"),
        macos = this.macosDto.required("$path.macos").toDomain("$path.macos"),
        needRestart = this.needRestartDto.required("$path.need_restart"),
        patchNote = this.patchNoteDto.orEmptyList(),
        tags = this.tagsDto.orEmptyList(),
        windows = this.windowsDto.required("$path.windows").toDomain("$path.windows"),
    )
}

private fun MacosDto.toDomain(path: String): Macos = Macos(
    hash = this.hashDto.required("$path.hash"),
    link = this.linkDto.required("$path.link"),
    size = this.sizeDto.required("$path.size"),
)

private fun WebDto.toDomain(path: String): Web = Web(
    hash = this.hashDto.required("$path.hash"),
    link = this.linkDto.required("$path.link"),
    size = this.sizeDto.required("$path.size"),
)

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
    if (version < MIN_SCHEMA_VERSION) error("$INVALID_FIELD_ERROR: $path = $version")
    return version
}

/**
 * Патч-ноуты и теги опциональны: отсутствие значения равносильно пустому списку.
 * */
private fun List<String>?.orEmptyList(): List<String> = this ?: emptyList()
