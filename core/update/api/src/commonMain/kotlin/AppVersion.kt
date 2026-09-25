data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val stage: Stage,
    val build: Int
) {
    enum class Stage {
        Alpha, Beta, RC, Release
    }
}

fun String.normalized(): AppVersion {
    TODO()
}
