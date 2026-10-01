package data.mapper

import data.dto.manifest.v2.ChannelsDto
import data.dto.manifest.v2.DevDto
import data.dto.manifest.v2.LastRejectionDto
import data.dto.manifest.v2.ProdDto
import data.dto.manifest.v2.TargetDataDto
import data.dto.manifest.v2.UpdateManifestDto
import domain.ManifestSupported
import domain.models.manifest.v2.Channels
import domain.models.manifest.v2.Dev
import domain.models.manifest.v2.LastRejection
import domain.models.manifest.v2.Prod
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
 * - Таргет, который считается неопубликованным при пустом хеше или
 * размере равном [MAX_UNPUBLISHED_SIZE].
 * */
internal fun UpdateManifestDto.toDomain(): UpdateManifest = UpdateManifest(
    channels = this.channelsDto.required("channels").toDomain(),
    schemaVersionDto = this.schemaVersionDto.toDomain()
)

private fun ChannelsDto.toDomain(): Channels = Channels(
    dev = this.devDto.required("channels.dev").toDomain(),
    prod = this.prodDto.required("channels.prod").toDomain(),
)

private fun DevDto.toDomain(): Dev = Dev(
    canary = this.canaryDto.required("channels.dev.canary").toDomain("channels.dev.canary"),
    rc = this.rcDto.required("channels.dev.rc").toDomain("channels.dev.rc"),
    lastRejection = this.lastRejectionDto.required("channels.dev.last_rejection").toDomain(),
)

private fun ProdDto.toDomain(): Prod = Prod(
    macosDto = this.macosDto.required("channels.prod.macos").toDomain("channels.prod.macos"),
    webDto = this.webDto.required("channels.prod.web").toDomain("channels.prod.web"),
    windowsDto = this.windowsDto.required("channels.prod.windows").toDomain("channels.prod.windows"),
)

private fun LastRejectionDto.toDomain(): LastRejection = LastRejection(
    dateDto = this.dateDto.required("channels.dev.last_rejection.date"),
    lastVersionDto = this.lastVersionDto.required("channels.dev.last_rejection.last_version"),
    reasonDto = this.reasonDto.required("channels.dev.last_rejection.reason"),
)

private fun TargetDataDto.toDomain(path: String): TargetData = TargetData(
    dateDto = this.dateDto.required("$path.date"),
    lastVersionDto = this.lastVersionDto.required("$path.last_version"),
    patchNoteDto = this.patchNoteDto.required("$path.patch_note"),
    tagsDto = this.tagsDto ?: emptyList(),
    link = this.linkDto.required("$path.link"),
    hash = this.hashDto.required("$path.hash"),
    size = this.sizeDto.required("$path.size"),
)

private fun<T> T?.required(path: String): T = this ?: error("$MISSING_FIELD_ERROR: $path")

private fun Int?.toDomain(): Int {
    TODO()
}
