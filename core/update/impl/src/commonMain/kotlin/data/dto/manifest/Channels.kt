package data.dto.manifest

@Serializable
data class Channels(
    @SerialName("dev") val devDto: Dev? = Dev(),
    @SerialName("prod") val prodDto: Prod? = Prod()
)