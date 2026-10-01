package domain.models.manifest.v2

data class UpdateManifest(
    val channels: Channels,
    val schemaVersionDto: Int,
)
