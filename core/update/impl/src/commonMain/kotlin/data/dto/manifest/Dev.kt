package data.dto.manifest


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Dev(
    @SerialName("current") val currentDto: Current? = Current(),
    @SerialName("last_rejection") val lastRejectionDto: LastRejection? = LastRejection(),
    @SerialName("stable") val stableDto: Stable? = Stable()
)