package data.dto.manifest.v2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifestDto(
    @SerialName("channels") val channelsDto: ChannelsDto? = null,
    @SerialName("schema_version") val schemaVersionDto: Int? = null
)
