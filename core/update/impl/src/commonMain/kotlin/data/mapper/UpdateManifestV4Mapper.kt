package data.mapper

import data.dto.manifest.v2.ChannelsDto
import data.dto.manifest.v2.DevDto
import data.dto.manifest.v2.LastRejectionDto
import data.dto.manifest.v2.ReleaseDto
import data.dto.manifest.v2.TargetDataDto
import data.dto.manifest.v2.UpdateManifestDto
import domain.ManifestSupported
import domain.models.manifest.v2.Channels
import domain.models.manifest.v2.Dev
import domain.models.manifest.v2.LastRejection
import domain.models.manifest.v2.Release
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest

private const val MISSING_FIELD_ERROR = "Отсутствует обязательное поле манифеста"
private const val INVALID_FIELD_ERROR = "Некорректное значение поля манифеста"
private val SCHEMA_VERSION = ManifestSupported.getManifestVersion("v2")
private const val MAX_UNPUBLISHED_SIZE = 0

/**
 * Маппинг манифеста обновлений на доменные модели.
 *
 * Маппинг строгий: отсутствие любого обязательного поля приводит к ошибке.
 * Исключения:
 *
 * - Патч-ноуты и теги (для них отсутствие значения равносильно пустому списку).
 * - Версия схемы, которая должна быть положительным числом.
 * - Дата и причина отклонения: сервер не заполняет их, пока отклонение не случилось.
 * - Таргет, который считается неопубликованным при пустом хеше или
 * неположительном размере: неопубликованный таргет переносится как [null]
 * и его поля не проверяются, потому что сервер их не присылает.
 * */
internal fun UpdateManifestDto.toDomain(): UpdateManifest = UpdateManifest(
    channels = this.channelsDto.required("channels").toDomain(),
    schemaVersion = this.schemaVersionDto.schemaVersion("schema_version"),
)

private fun ChannelsDto.toDomain(): Channels = Channels(
    dev = this.devDto.required("channels.dev").toDomain(),
    prod = this.prodDto.required("channels.prod").toDomain("channels.prod"),
)

private fun DevDto.toDomain(): Dev = Dev(
    canary = this.canaryDto.required("channels.dev.canary").toDomain("channels.dev.canary"),
    rc = this.rcDto.required("channels.dev.rc").toDomain("channels.dev.rc"),
    lastRejection = this.lastRejectionDto.required("channels.dev.last_rejection").toDomain(),
)

private fun ReleaseDto.toDomain(path: String): Release = Release(
    macos = this.macosDto.required("$path.macos").toDomain("$path.macos"),
    web = this.webDto.required("$path.web").toDomain("$path.web"),
    windows = this.windowsDto.required("$path.windows").toDomain("$path.windows"),
)

private fun LastRejectionDto.toDomain(): LastRejection = LastRejection(
    date = this.dateDto,
    lastVersion = this.lastVersionDto.required("channels.dev.last_rejection.last_version"),
    reason = this.reasonDto,
)

/**
 * Таргет переносится на доменную модель только когда он опубликован, то есть
 * сервер прислал непустой хеш и положительный размер. Неопубликованный таргет
 * возвращает [null], а его остальные поля не проверяются: сервер их не присылает.
 * */
private fun TargetDataDto.toDomain(path: String): TargetData? {
    val hash = this.hashDto.orEmpty()
    val size = this.sizeDto

    if (hash.isBlank() || size == null || size <= MAX_UNPUBLISHED_SIZE) return null

    return TargetData(
        version = this.versionDto.required("$path.version"),
        patchNote = this.patchNoteDto.orEmptyList(),
        tags = this.tagsDto.orEmptyList(),
        date = this.dateDto.required("$path.date"),
        link = this.linkDto.required("$path.link"),
        hash = hash,
        size = size,
    )
}

/**
 * Обязательное поле манифеста: [null] означает, что сервер его не прислал.
 * */
private fun <T> T?.required(path: String): T = this ?: error("$MISSING_FIELD_ERROR: $path")

/**
 * Версия схемы манифеста: должна быть не меньше версии, описанной DTO.
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
