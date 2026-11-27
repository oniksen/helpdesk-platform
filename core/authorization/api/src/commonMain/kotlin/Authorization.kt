import user.UserData

/** Контракт авторизации на Helpdesk и управления сессией пользователя. */
interface Authorization : AuthSession {
    /**
     * Авторизация на *Helpdesk* через данные платформы *MAX*.
     * */
    suspend fun helpdeskAuth(maxInitData: String): AuthResponse

    /**
     * Стандартная авторизация на *Helpdesk*.
     * */
    suspend fun helpdeskAuth(email: String, password: String): AuthResponse

    /** Получить от сервера авторизационные данные пользователя */
    suspend fun fetchUserData(email: String): UserData

    /**
     * Получить токен авторизации на платформе Helpdesk.
     *
     * @throws IllegalStateException Если токена не существует.
     * */
    fun getToken(): String

    /**
     * Получить данные авторизованного пользователя.
     *
     * @throws IllegalStateException Если данные не существуют.
     * */
    fun getUserData(): UserData
}
