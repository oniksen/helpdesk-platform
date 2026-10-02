package domain.models.manifest.v2

data class TargetData(
    val version: String,
    val patchNote: List<String>,
    val tags: List<String>,
    val date: String,
    val link: String,
    val hash: String,
    val size: Long,
)
