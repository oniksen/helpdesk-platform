package domain.models.manifest

data class Prod(
    val date: String,
    val lastVersion: String,
    val macos: Macos,
    val patchNote: List<String>,
    val tags: List<String>,
    val web: Web?,
    val windows: Windows,
)
