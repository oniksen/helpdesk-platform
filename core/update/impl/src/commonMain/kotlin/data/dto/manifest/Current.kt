package data.dto.manifest


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Current(
    @SerialName("date") val dateDto: String? = null,
    @SerialName("last_version") val lastVersionDto: String? = null,
    @SerialName("macos") val macosDto: Macos? = null,
    @SerialName("need_restart") val needRestartDto: Boolean? = null,
    @SerialName("patch_note") val patchNoteDto: List<String?>? = null,
    @SerialName("tags") val tagsDto: List<String?>? = null,
    @SerialName("web") val webDto: Web? = null,
    @SerialName("windows") val windowsDto: Windows? = null
)
