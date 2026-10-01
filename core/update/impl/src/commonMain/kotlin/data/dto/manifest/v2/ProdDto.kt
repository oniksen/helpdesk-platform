package data.dto.manifest.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProdDto(
    @SerialName("macos") val macosDto: TargetDataDto? = null,
    @SerialName("web") val webDto: TargetDataDto? = null,
    @SerialName("windows") val windowsDto: TargetDataDto? = null
)
