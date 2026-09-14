package data.mapper

import data.dto.auth.response.AuthResponseDto
import AuthResponse

internal fun AuthResponseDto.toDomain(): AuthResponse = AuthResponse(
    bearerToken = this.bearerTokenDto ?: error("Не удалось получить токен авторизации"),
    cookies = this.cookiesDto,
    userId = this.userIdDto ?: error("Не удалось получить ID пользователя"),
    email = this.email ?: error("Не удалось получить email пользователя")
)