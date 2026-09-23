package data.mapper

import auth.AuthData
import data.dto.authstate.AuthStateDto
import user.AccessLevel
import user.UserData
import user.UserRole

internal fun AuthStateDto.toAuthData(): AuthData? {
    val maxInitData = this.maxInitData ?: return null
    val token = this.token ?: return null
    return AuthData(
        maxInitData = maxInitData,
        token = token,
        cookies = cookies,
    )
}

internal fun AuthStateDto.toUserData(): UserData? {
    val userId = this.userId ?: return null
    val userName = this.userName ?: return null
    val userEmail = this.userEmail ?: return null
    return UserData(
        id = userId,
        name = userName,
        lastName = userLastName,
        patronymic = userPatronymic,
        email = userEmail,
        accessLevel = runCatching { AccessLevel.valueOf(accessLevel.orEmpty()) }
            .getOrDefault(AccessLevel.EMPLOYEE),
        role = runCatching { UserRole.valueOf(userRole.orEmpty()) }
            .getOrDefault(UserRole.NONE),
        buildings = buildings,
        services = services,
    )
}
