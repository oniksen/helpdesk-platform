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

    companion object {
        const val NORMALIZE_ERROR = "Не удалось распарсить версию"
        const val INVALID_NUMBER_ERROR = "Номер версии не может быть отрицательным"
        const val VERSION_PATTERN = """^(\d+)\.(\d+)\.(\d+)-([a-zA-Z]+)\.(\d+)$"""
    }
}

/**
 * Получить типизированную версию приложения.
 *
 * @throws IllegalStateException Если какой-либо параметр не подходит по паттерну.
 * */
fun String.normalized(): AppVersion {
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

private fun String.toIntOrSendError(): Int = toIntOrNull() ?: error(AppVersion.NORMALIZE_ERROR)

/**
 * Прокси-метод для пробрасывания единообразных исключений при нормализации версии.
 * */
private fun String.toStageOrSendError(): AppVersion.Stage =
    AppVersion.Stage.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: error(AppVersion.NORMALIZE_ERROR)
