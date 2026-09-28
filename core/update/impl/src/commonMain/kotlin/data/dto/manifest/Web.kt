package data.dto.manifest


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Web(
    @SerialName("hash") val hashDto: String? = null,
    @SerialName("link") val linkDto: String? = null,
    @SerialName("size") val sizeDto: Int? = null
)