package domain.models.manifest.v1

data class UpdateManifest(
    val channels: Channels,
    val schemaVersion: Int,
)
