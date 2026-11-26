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

    sealed class ValidationError(
        message: String? = null,
        userMessageId: Int? = null,
        cause: Throwable? = null
    ) : AppException(message, userMessageId, cause) {

        /**
         * Ошибка маппинга DTO -> Domain или парсинга данных.
         * Используется, когда сырые данные принципиально невозможно превратить в доменный объект.
         *
         * @property field Имя свойства/поля, в котором произошел сбой (например, "birth_date").
         */
        class MappingFailed(val field: String,
            message: String? = "Ошибка обработки данных поля $field"
        ) : ValidationError(message = message)

        /**
         * Ошибка валидации конкретного поля (например, пустой email, короткий пароль).
         * Отлично подходит для UI, чтобы подсветить конкретный инпут.
         *
         * @property field Название поля (например, "email").
         * @property details Конкретная причина (например, "Неверный формат").
         *                   Вместо String здесь может быть enum или ID строки.
         */
        class InvalidField(
            val field: String,
            val details: String,
            userMessageId: Int? = null
        ) : ValidationError(message = details, userMessageId = userMessageId)

        /**
         * Комплексная ошибка формы, когда неверно сочетание нескольких полей.
         * (Например: "Дата окончания не может быть раньше даты начала").
         *
         * @property fields Список полей, вовлеченных в ошибку.
         */
        class InvalidForm(
            val fields: List<String>,
            message: String,
            userMessageId: Int? = null
        ) : ValidationError(message = message, userMessageId = userMessageId)

        /**
         * Общая ошибка входных параметров функции/Use Case.
         * Используется, когда UI передал некорректный аргумент общего характера.
         */
        class InvalidArgument(
            message: String
        ) : ValidationError(message = message)
    }

    sealed class UpdateError(
        message: String? = null,
        userMessageId: Int? = null,
        cause: Throwable? = null
    ) : AppException(message, userMessageId, cause) {

        /**
         * Ошибка при проверке обновлений или работе с каналами/манифестом (например, канал не найден или устарел).
         */
        class CheckFailed(
            val channel: String? = null,
            message: String? = "Не удалось проверить наличие обновлений",
            userMessageId: Int? = null,
            cause: Throwable? = null
        ) : UpdateError(message, userMessageId, cause)

        /**
         * Ошибка при скачивании файла обновления (например, обрыв загрузки, нехватка места на диске).
         */
        class DownloadFailed(
            message: String? = "Ошибка при загрузке обновления",
            userMessageId: Int? = null,
            cause: Throwable? = null
        ) : UpdateError(message, userMessageId, cause)

        /**
         * Файл скачался, но поврежден (не совпала контрольная сумма / хэш или подпись).
         */
        class CorruptedPackage(
            message: String? = "Файл обновления поврежден",
            userMessageId: Int? = null
        ) : UpdateError(message, userMessageId)

        /**
         * Ошибка при установке (например, нет разрешений на установку из неизвестных источников или отказ системы).
         */
        class InstallFailed(
            message: String? = "Не удалось установить обновление",
            userMessageId: Int? = null,
            cause: Throwable? = null
        ) : UpdateError(message, userMessageId, cause)
    }

    sealed class LoginError(
        message: String? = null,
        userMessageId: Int? = null,
        cause: Throwable? = null
    ) : AppException(message, userMessageId, cause) {

        /** Оболочка MAX сервиса по какой-то причине недоступна. */
        class MaxLoginUnavailable(
            message: String? = "MAX авторизация недоступна.",
            userMessageId: Int? = null,
            cause: Throwable? = null
        ) : LoginError(message, userMessageId, cause)

        /** Пользователь указал неправильный логин или пароль. */
        class IncorrectCredentials(
            message: String? = "Неверный логин или пароль",
            userMessageId: Int? = null,
            cause: Throwable? = null
        ) : LoginError(message, userMessageId, cause)
    }

    class Unknown(cause: Throwable?) : AppException(message = cause?.message, cause = cause)
}
