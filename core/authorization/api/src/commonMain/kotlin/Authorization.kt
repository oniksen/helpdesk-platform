interface Authorization {
    /**
     * Получение текущих данных пользователя из платформы MAX.
     * */
    suspend fun getMaxInitData(): String
}