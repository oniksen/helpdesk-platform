package domain.model

/**
 * Состояние процесса обновления приложения.
 *
 * Каждый этап (Loading, Verification, IntegrityChecking, Unpacking, Installation)
 * содержит [CurrentStageState], отражающий прогресс внутри этапа.
 * [Restarting] содержит количество секунд до перезапуска.
 */
sealed class UpdateState {
    /** Ожидание — обновление не запущено. */
    data class Idle(val stage: CurrentStageState) : UpdateState()

    /** Загрузка архива обновления. */
    data class Loading(val stage: CurrentStageState) : UpdateState()

    /** Проверка подлинности (размер и SHA-256 хеш). */
    data class Verification(val stage: CurrentStageState) : UpdateState()

    /** Проверка целостности zip-архива. */
    data class IntegrityChecking(val stage: CurrentStageState) : UpdateState()

    /** Распаковка архива во временную директорию. */
    data class Unpacking(val stage: CurrentStageState) : UpdateState()

    /** Замена файлов приложения. */
    data class Installation(val stage: CurrentStageState) : UpdateState()

    /** Очистка временных файлов. */
    data class CleaningTemporaryFiles(val stage: CurrentStageState) : UpdateState()

    /** Обратный отсчёт до перезапуска приложения. */
    data class Restarting(val remaining: Int) : UpdateState()
}

/**
 * Состояние текущего этапа обновления.
 *
 * - [Idle]: этап не начат.
 * - [Progress]: этап выполняется с указанным прогрессом (0.0–100.0).
 * - [Error]: на этапе произошла ошибка; [Error.message] — причина для лога.
 * - [Success]: этап завершён успешно.
 */
sealed class CurrentStageState {
    data object Idle : CurrentStageState()
    data class Progress(val value: Float? = null) : CurrentStageState()
    data class Error(val message: String? = null) : CurrentStageState()
    data object Success : CurrentStageState()
}
