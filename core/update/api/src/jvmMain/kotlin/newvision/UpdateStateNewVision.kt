package newvision

/**
 * Состояние процесса обновления приложения.
 *
 * Каждый этап (Loading, Verification, IntegrityChecking, Unpacking, Installation)
 * содержит [CurrentStageState], отражающий прогресс внутри этапа.
 * [Restarting] содержит количество секунд до перезапуска.
 */
sealed interface UpdateStateNewVision {
    /** Ожидание — обновление не запущено. */
    data object Idle : UpdateStateNewVision

    /** Загрузка архива обновления. */
    data class Loading(val stage: CurrentStageState) : UpdateStateNewVision

    /** Проверка подлинности (размер и SHA-256 хеш). */
    data class Verification(val stage: CurrentStageState) : UpdateStateNewVision

    /** Проверка целостности zip-архива. */
    data class IntegrityChecking(val stage: CurrentStageState) : UpdateStateNewVision

    /** Распаковка архива во временную директорию. */
    data class Unpacking(val stage: CurrentStageState) : UpdateStateNewVision

    /** Замена файлов приложения. */
    data class Installation(val stage: CurrentStageState) : UpdateStateNewVision

    /** Очистка временных файлов. */
    data class CleaningTemporaryFiles(val stage: CurrentStageState) : UpdateStateNewVision

    /** Обратный отсчёт до перезапуска приложения. */
    data class Restarting(val remaining: Int) : UpdateStateNewVision
}

/**
 * Состояние текущего этапа обновления.
 *
 * - [Idle]: этап не начат.
 * - [Progress]: этап выполняется с указанным прогрессом (0.0–100.0).
 * - [Error]: на этапе произошла ошибка; [Error.message] — причина для лога.
 * - [Success]: этап завершён успешно.
 */
sealed interface CurrentStageState {
    data object Idle : CurrentStageState
    data class Progress(val value: Float? = null) : CurrentStageState
    data class Error(val message: String? = null) : CurrentStageState
    data object Success : CurrentStageState
}
