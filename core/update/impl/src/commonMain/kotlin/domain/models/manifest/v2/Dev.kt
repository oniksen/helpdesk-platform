package domain.models.manifest.v2

data class Dev(
    val canary: Release,
    val rc: Release,
    val lastRejection: LastRejection,
)
