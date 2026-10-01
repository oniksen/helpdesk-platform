package domain.models.manifest.v2

data class LastRejection(
    val dateDto: String,
    val lastVersionDto: String,
    val reasonDto: String,
)
