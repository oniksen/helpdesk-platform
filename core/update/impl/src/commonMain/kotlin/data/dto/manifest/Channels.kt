package data.dto.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Channels(
    @SerialName("dev") val devDto: Dev? = Dev(),
    @SerialName("prod") val prodDto: Prod? = Prod()
)