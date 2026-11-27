import auth.AuthData
import user.UserData

// Контракт объединяет вход, восстановление сессии и очистку при выходе.
@Suppress("TooManyFunctions")
interface Authorization {
    /**
     * Получение текущих данных пользователя из платформы MAX.
     * */
    /*suspend fun getMaxInitData(): MaxAuthorizationResult*/

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

    /** Сохранить данные авторизованного пользователя. */
    fun saveUser(userData: UserData)

    /** Сохранить данные для авторизации запросов на Helpdesk. */
    fun saveAuthData(authData: AuthData)

    /**
     * Установить значение авто входа в приложение.
     * */
    fun saveAutoLogin(value: Boolean)

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

    /** Получить параметр автоматического входа в приложение */
    fun getAutoLogin(): Boolean

    /** Сохранить авторизационные данные для восстановления после перезапуска приложения. */
    fun persistAuthState()

    /**
     * Восстановить сохранённые авторизационные данные.
     *
     * @return true, если данные авторизации и пользователя восстановлены.
     * */
    fun restoreAuthState(): Boolean

    /** Удалить сохранённую сессию при выходе, если авто-вход отключён. */
    fun clearAuthStateOnExit()
}