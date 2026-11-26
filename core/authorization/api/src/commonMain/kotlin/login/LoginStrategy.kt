package login

import AuthResponse

/**
 * Платформенная стратегия входа.
 *
 * В DI-графе будет только одна реализация:
 *  * Web - регистрирует MAX-стратегию.
 *  * JVM - регистрирует парольную стратегию.
 * */
interface LoginStrategy {
    val mode: LoginMode

    /**
     * Единственная точка вызова авторизации.
     *
     * @throws [AppException.LoginError]
     * */
    suspend fun authorize(credentials: LoginCredentials? = null): AuthResponse
}
