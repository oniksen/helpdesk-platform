package data.dto.manifest.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WindowsDto(
    @SerialName("hash") val hashDto: String? = null,
    @SerialName("link") val linkDto: String? = null,
    @SerialName("size") val sizeDto: Int? = null
)
