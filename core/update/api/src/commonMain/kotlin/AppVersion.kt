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
    // 0.1.1-alpha.1
    val (leading, trail) = this.split("-").let {
        it.map { part -> part.split(".") }
    }
    val major = leading[0].toIntOrNull() ?: -1
    val minor = leading[1].toIntOrNull() ?: -1
    val patch = leading[2].toIntOrNull() ?: -1
    val stage = trail[0]
    val build = trail[1].toIntOrNull() ?: -1

    TODO()
}
