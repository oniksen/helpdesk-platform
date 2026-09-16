// domain/exception/AppException.kt
// domain/exception/AppException.kt
sealed class AppException(
    message: String? = null,
    val userMessageId: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause) { // Передаем аргументы позиционно, без имён параметров

    sealed class Network(
        message: String? = null,
        userMessageId: Int? = null,
        cause: Throwable? = null
    ) : AppException(message, userMessageId, cause) {

        class NoInternet(message: String? = "Нет подключения к интернету") : Network(message = message)
        class ServerError(val code: Int, message: String?) : Network(message = message)
        class Timeout(message: String? = "Время ожидания истекло") : Network(message = message)
    }

    class Unknown(cause: Throwable?) : AppException(message = cause?.message, cause = cause)
}
