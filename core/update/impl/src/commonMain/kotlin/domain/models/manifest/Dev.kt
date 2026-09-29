package domain.models.manifest

data class Dev(
    val current: Current,
    val lastRejection: LastRejection,
    val stable: Stable
)
