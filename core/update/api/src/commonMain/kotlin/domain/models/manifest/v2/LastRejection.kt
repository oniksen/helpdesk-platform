package domain.models.manifest.v2

/**
 * Отклонённая сборка.
 *
 * Сервер не заполняет [date] и [reason], пока отклонение не случилось, поэтому
 * оба поля опциональны. Версия отклонённой сборки всегда присутствует.
 * */
data class LastRejection(
    val date: String?,
    val lastVersion: String,
    val reason: String?,
)
