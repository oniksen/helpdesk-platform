package domain.models.manifest

data class UpdateManifest(
    val channels: Channels,
    val schemaVersion: Int,
)
