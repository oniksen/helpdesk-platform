package data.dto.manifest


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifestDto(
    @SerialName("channels") val channelsDto: Channels? = Channels(),
    @SerialName("schema_version") val schemaVersionDto: Int? = 0
)