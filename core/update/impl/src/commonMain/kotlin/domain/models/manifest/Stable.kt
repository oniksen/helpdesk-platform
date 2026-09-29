package domain.models.manifest

data class Stable(
    val date: String,
    val lastVersion: String,
    val macos: Macos,
    val needRestart: Boolean,
    val patchNote: List<String>,
    val tags: List<String>,
    val windows: Windows
)
