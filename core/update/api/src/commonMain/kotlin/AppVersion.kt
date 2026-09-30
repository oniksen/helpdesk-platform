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

    init {
        require(major >= 0 && minor >= 0 && patch >= 0 && build >= 0) { INVALID_NUMBER_ERROR }
    }

    override fun toString(): String = "$major.$minor.$patch-${stage.name}.$build"

    operator fun compareTo(other: AppVersion): Int {
        return compareValuesBy(
            this, other,
            AppVersion::major,
            AppVersion::minor,
            AppVersion::patch,
            AppVersion::stage,
            AppVersion::build
        )
    }

    companion object {
        const val NORMALIZE_ERROR = "Не удалось распарсить версию"
        const val INVALID_NUMBER_ERROR = "Номер версии не может быть отрицательным"
        const val VERSION_PATTERN = """^(\d+)\.(\d+)\.(\d+)-([a-zA-Z]+)\.(\d+)$"""
        const val TECHNICAL_PATTERN = """^(\d+)\.(\d+)\.(\d+)$"""
    }
}

/**
 * Получить типизированную версию приложения из user-friendly написания
 * (например: 1.1.0-alpha.1).
 *
 * @throws IllegalStateException Если какой-либо параметр не подходит по паттерну.
 * */
fun String.normalizeFromUi(): AppVersion {
    val matchResult = AppVersion.VERSION_PATTERN.toRegex().matchEntire(this)
        ?: error(AppVersion.NORMALIZE_ERROR)

    return matchResult.destructured.let { (major, minor, patch, stage, build) ->
        AppVersion(
            major = major.toIntOrSendError(),
            minor = minor.toIntOrSendError(),
            patch = patch.toIntOrSendError(),
            stage = stage.toStageOrSendError(),
            build = build.toIntOrSendError(),
        )
    }
}

fun String.normalizeFromTechnical(): AppVersion {
    val matchResult = AppVersion.TECHNICAL_PATTERN.toRegex().matchEntire(this)
        ?: error(AppVersion.NORMALIZE_ERROR)

    return matchResult.destructured.let { (major, minor, collapsed) ->
        collapsed.toIntOrSendError().let {
            val patch = it / 10_000
            var offset = it % 10_000
            val stage = when (offset / 1_000) {
                1 -> AppVersion.Stage.Alpha
                2 -> AppVersion.Stage.Beta
                3 -> AppVersion.Stage.RC
                else -> AppVersion.Stage.Release
            }
            offset %= 1_000
            val build = offset

            AppVersion(
                major = major.toIntOrSendError(),
                minor = minor.toIntOrSendError(),
                patch = patch,
                stage = stage,
                build = build,
            )
        }
    }
}

private fun String.toIntOrSendError(): Int = toIntOrNull() ?: error(AppVersion.NORMALIZE_ERROR)

/**
 * Прокси-метод для пробрасывания единообразных исключений при нормализации версии.
 * */
private fun String.toStageOrSendError(): AppVersion.Stage =
    AppVersion.Stage.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: error(AppVersion.NORMALIZE_ERROR)
