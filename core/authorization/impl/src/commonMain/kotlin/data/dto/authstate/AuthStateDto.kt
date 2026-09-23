package data.dto.authstate

import kotlinx.serialization.Serializable

/**
 * Плоский DTO состояния авторизации для сохранения между перезагрузками страницы.
 * Перечисления представлены строками по имени.
 * */
@Serializable
internal data class AuthStateDto(
    val maxInitData: String? = null,
    val token: String? = null,
    val cookies: String? = null,
    val userId: Int? = null,
    val userName: String? = null,
    val userLastName: String? = null,
    val userPatronymic: String? = null,
    val userEmail: String? = null,
    val accessLevel: String? = null,
    val userRole: String? = null,
    val buildings: List<Int> = emptyList(),
    val services: List<Int> = emptyList(),
)
