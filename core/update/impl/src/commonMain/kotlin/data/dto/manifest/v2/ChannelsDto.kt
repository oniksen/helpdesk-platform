package data.dto.manifest.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelsDto(
    @SerialName("dev") val devDto: DevDto? = null,
    @SerialName("prod") val prodDto: ProdDto? = null
)
