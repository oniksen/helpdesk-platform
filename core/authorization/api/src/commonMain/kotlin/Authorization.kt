import auth.AuthData
import user.UserData

interface Authorization {
    /**
     * Получение текущих данных пользователя из платформы MAX.
     * */
    suspend fun getMaxInitData(): MaxAuthorizationResult

    /**
     * Авторизация на *Helpdesk* через данные платформы *MAX*.
     * */
    suspend fun helpdeskAuth(maxInitData: String): AuthResponse

    /** Получить от сервера авторизационные данные пользователя */
    suspend fun fetchUserData(email: String): UserData

    /** Сохранить данные авторизованного пользователя. */
    fun saveUser(userData: UserData)

    /** Сохранить данные для авторизации запросов на Helpdesk. */
    fun saveAuthData(authData: AuthData)

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

    /** Сохранить авторизационные данные для восстановления после перезагрузки страницы. */
    fun persistAuthState()

    /**
     * Восстановить сохранённые авторизационные данные.
     *
     * @return true, если данные авторизации и пользователя восстановлены.
     * */
    fun restoreAuthState(): Boolean
}