package domain.models.manifest

data class LastRejection(
    val date: String,
    val lastVersion: String,
    val reason: String
)
