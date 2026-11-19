package domain.model

/**
 * Состояние процесса установки.
 *
 * Этапы соответствуют проверенному пайплайну: загрузка → верификация →
 * проверка целостности → распаковка → установка → очистка → перезапуск.
 * Каждый этап содержит [CurrentStageState] с прогрессом или ошибкой.
 */
sealed interface UpdateState {
    /** Ожидание — установка не запущена. */
    data object Idle : UpdateState

    /** Загрузка архива обновления. */
    data class Loading(val stage: CurrentStageState) : UpdateState

    /** Проверка подлинности (размер и SHA-256 хеш). */
    data class Verification(val stage: CurrentStageState) : UpdateState

    /** Проверка целостности zip-архива. */
    data class IntegrityChecking(val stage: CurrentStageState) : UpdateState

    /** Распаковка архива во временную директорию. */
    data class Unpacking(val stage: CurrentStageState) : UpdateState

    /** Замена файлов приложения. */
    data class Installation(val stage: CurrentStageState) : UpdateState

    /** Очистка временных файлов. */
    data class CleaningTemporaryFiles(val stage: CurrentStageState) : UpdateState

    /** Обратный отсчёт до перезапуска приложения. */
    data class Restarting(val remaining: Int) : UpdateState
}

/**
 * Состояние текущего этапа обновления.
 *
 * - [Idle]: этап не начат.
 * - [Progress]: этап выполняется; [Progress.value] — прогресс 0..100.
 * - [Error]: на этапе произошла ошибка; [Error.message] — причина для лога/UI.
 * - [Success]: этап завершён успешно.
 */
sealed interface CurrentStageState {
    data object Idle : CurrentStageState
    data class Progress(val value: Float? = null) : CurrentStageState
    data class Error(val message: String? = null) : CurrentStageState
    data object Success : CurrentStageState
}
