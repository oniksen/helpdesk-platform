package data.dto.manifest.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StableDto(
    @SerialName("date") val dateDto: String? = null,
    @SerialName("last_version") val lastVersionDto: String? = null,
    @SerialName("macos") val macosDto: MacosDto? = null,
    @SerialName("patch_note") val patchNoteDto: List<String>? = null,
    @SerialName("tags") val tagsDto: List<String>? = null,
    @SerialName("web") val webDto: WebDto? = null,
    @SerialName("windows") val windowsDto: WindowsDto? = null
)
