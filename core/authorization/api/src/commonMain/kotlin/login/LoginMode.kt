package login

/** Каким образом платформа получает сессию Helpdesk. */
enum class LoginMode {
    /** Без формы. Данные приходят от MAX окружения. */
    Silent,
    /** Форма Email + пароль. */
    Credentials
}
