package domain.models.manifest.v1

data class LastRejection(
    val date: String,
    val lastVersion: String,
    val reason: String
)
