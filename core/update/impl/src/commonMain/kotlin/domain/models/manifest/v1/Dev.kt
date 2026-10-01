package domain.models.manifest.v1

data class Dev(
    val current: Current,
    val lastRejection: LastRejection,
    val stable: Stable
)
