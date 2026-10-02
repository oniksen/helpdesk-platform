package domain.models.manifest.v1

data class Stable(
    val date: String,
    val lastVersion: String,
    val macos: Macos,
    val patchNote: List<String>,
    val tags: List<String>,
    val web: Web?,
    val windows: Windows
)
