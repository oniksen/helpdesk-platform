package data.dto.manifest.v1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevDto(
    @SerialName("current") val currentDto: CurrentDto? = null,
    @SerialName("last_rejection") val lastRejectionDto: LastRejectionDto? = null,
    @SerialName("stable") val stableDto: StableDto? = null
)
