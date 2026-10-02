package data.dto.manifest.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevDto(
    @SerialName("canary") val canaryDto: ReleaseDto? = null,
    @SerialName("rc") val rcDto: ReleaseDto? = null,
    @SerialName("last_rejection") val lastRejectionDto: LastRejectionDto? = null,
)
