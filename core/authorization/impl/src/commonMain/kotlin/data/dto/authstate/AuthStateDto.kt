package data.dto.authstate

import kotlinx.serialization.Serializable

/**
 * Минимальное состояние авторизации для восстановления сессии.
 *
 * В файле хранятся только токен, email и флаг авто-входа. Данные пользователя
 * при восстановлении запрашиваются заново по email.
 * */
@Serializable
internal data class AuthStateDto(
    val autoLogin: Boolean = false,
    val token: String? = null,
    val userEmail: String? = null,
)
