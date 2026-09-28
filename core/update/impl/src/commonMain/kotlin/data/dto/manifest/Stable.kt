package data.dto.manifest


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Stable(
    @SerialName("date") val dateDto: String? = "",
    @SerialName("last_version") val lastVersionDto: String? = "",
    @SerialName("macos") val macosDto: Macos? = Macos(),
    @SerialName("need_restart") val needRestartDto: Boolean? = false,
    @SerialName("patch_note") val patchNoteDto: List<String>? = listOf(),
    @SerialName("tags") val tagsDto: List<String>? = listOf(),
    @SerialName("windows") val windowsDto: Windows? = Windows()
)