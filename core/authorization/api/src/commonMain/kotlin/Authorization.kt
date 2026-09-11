interface Authorization {
    /**
     * Получение текущих данных пользователя из платформы MAX.
     * */
    suspend fun getMaxInitData(): MaxAuthorizationResult

    /**
     * Авторизация на *Helpdesk* через данные платформы *MAX*.
     * */
    suspend fun helpdeskAuth(maxInitData: String): AuthResponse
}