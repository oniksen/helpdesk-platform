package domain.exception

// domain/exception/AppException.kt
/*
sealed class AppException : Exception {
    val userMessageId: Int?

    // Конструктор по умолчанию + с сообщением
    constructor(message: String? = null, userMessageId: Int? = null) : super(message) {
        this.userMessageId = userMessageId
    }

    // Дополнительный конструктор для ошибок, у которых есть cause (причина)
    constructor(message: String?, cause: Throwable?, userMessageId: Int? = null) : super(message, cause) {
        this.userMessageId = userMessageId
    }

    sealed class Network : AppException() {
        class NoInternet(message: String? = "Нет подключения к интернету") : Network()
        class ServerError(val code: Int, message: String?) : Network()
        class Timeout(message: String? = "Время ожидания истекло") : Network()
    }

    // Для Unknown передаем и сообщение, и саму причину (cause), чтобы сохранить стэк-трейс
    class Unknown(cause: Throwable?) : AppException(message = cause?.message, cause = cause)
}
*/
