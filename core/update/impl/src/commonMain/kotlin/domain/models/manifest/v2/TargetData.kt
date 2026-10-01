package domain.models.manifest.v2

data class TargetData(
    val dateDto: String,
    val lastVersionDto: String,
    val patchNoteDto: List<String>,
    val tagsDto: List<String>,
    val link: String,
    val hash: String,
    val size: Long,
)
