package data.dto.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastRejectionDto(
    @SerialName("date") val dateDto: String? = null,
    @SerialName("last_version") val lastVersionDto: String? = null,
    @SerialName("reason") val reasonDto: String? = null
)
