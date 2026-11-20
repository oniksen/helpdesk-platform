package domain.model

import AppException

/**
 * Состояние проверки обновления.
 *
 * [Idle] — проверка ещё не запускалась; [Loading] — выполняется.
 * Остальные состояния терминальные: они публикуются в `checkUpdateState`
 * и не сбрасываются в [Idle].
 */
sealed interface CheckUpdateState {
    /** Проверка ещё не запускалась. */
    data object Idle : CheckUpdateState

    /** Проверка выполняется. */
    data object Loading : CheckUpdateState

    /** Найдено обновление. */
    data class Available(val update: AvailableUpdate) : CheckUpdateState

    /** Установлена актуальная (или более новая) версия. */
    data object IsUpToDate : CheckUpdateState

    /** Обновление проверить можно, но ставить нечего. */
    data class Unavailable(val reason: UnavailableReason) : CheckUpdateState

    /** Проверка не удалась: сеть, HTTP, парсинг, невалидная версия. */
    data class Error(val error: AppException.UpdateError) : CheckUpdateState
}

/** Почему обновление недоступно, хотя проверка выполнена. */
enum class UnavailableReason {
    /** ОС не поддерживает JVM-обновления (Linux и т.п.). */
    UNSUPPORTED_PLATFORM,

    /** Для текущего канала и платформы нет опубликованного таргета. */
    NO_PUBLISHED_BUILD,
}
