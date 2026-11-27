import auth.AuthData
import user.UserData

/**
 * Управление сессией пользователя: сохранение, восстановление и очистка.
 *
 * Вынесено из [Authorization], чтобы контракт авторизации не разрастался.
 * */
interface AuthSession {
    /** Сохранить данные авторизованного пользователя. */
    fun saveUser(userData: UserData)

    /** Сохранить данные для авторизации запросов на Helpdesk. */
    fun saveAuthData(authData: AuthData)

    /** Установить значение авто-входа в приложение. */
    fun saveAutoLogin(value: Boolean)

    /** Получить параметр автоматического входа в приложение. */
    fun getAutoLogin(): Boolean

    /** Сохранить авторизационные данные для восстановления после перезапуска. */
    fun persistAuthState()

    /**
     * Восстановить сессию: токен и email берутся из хранилища,
     * данные пользователя запрашиваются заново.
     *
     * @return true, если сессия восстановлена.
     * */
    suspend fun restoreAuthState(): Boolean

    /** Удалить сохранённую сессию из постоянного хранилища. */
    fun clearAuthState()
}
