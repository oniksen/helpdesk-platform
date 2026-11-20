package data.dto.manifest.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Набор таргетов одной ветки обновлений: prod, dev.canary или dev.rc.
 * */
@Serializable
data class ReleaseDto(
    @SerialName("macos") val macosDto: TargetDataDto? = null,
    @SerialName("web") val webDto: TargetDataDto? = null,
    @SerialName("windows") val windowsDto: TargetDataDto? = null,
)
