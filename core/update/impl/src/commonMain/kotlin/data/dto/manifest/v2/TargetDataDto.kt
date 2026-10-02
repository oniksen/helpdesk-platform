package data.dto.manifest.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Таргет одной платформы.
 *
 * Неопубликованный таргет сервер описывает пустыми [dateDto], [hashDto] и
 * [sizeDto], поэтому все поля nullable.
 * */
@Serializable
data class TargetDataDto(
    @SerialName("date") val dateDto: String? = null,
    @SerialName("version") val versionDto: String? = null,
    @SerialName("patch_note") val patchNoteDto: List<String>? = null,
    @SerialName("tags") val tagsDto: List<String>? = null,
    @SerialName("link") val linkDto: String? = null,
    @SerialName("hash") val hashDto: String? = null,
    @SerialName("size") val sizeDto: Long? = null,
)
