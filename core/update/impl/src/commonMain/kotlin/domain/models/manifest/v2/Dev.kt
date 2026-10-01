package domain.models.manifest.v2

data class Dev(
    val canary: TargetData,
    val rc: TargetData,
    val lastRejection: LastRejection,
)
