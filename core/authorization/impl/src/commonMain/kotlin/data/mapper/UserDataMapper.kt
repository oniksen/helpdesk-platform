package data.mapper

import user.UserData
import data.dto.user.UserDataDto
import user.AccessLevel
import user.UserRole

private const val DISPATCHER_GROUP_ID = 5
private const val MANAGER_GROUP_ID = 7

internal fun UserDataDto.toDomain(): UserData {
    val message = this.messageDto ?: error("Не удалось получить данные для авторизации")

    val accessLevel = AccessLevel.valueOf(message.accessLevelDto ?: AccessLevel.EMPLOYEE.name)
    val groups = message.userGroupsDto
        ?.mapNotNull { it?.toIntOrNull() }
        ?: emptyList()
    val userRole = when {
        groups.contains(DISPATCHER_GROUP_ID) -> UserRole.DISPATCHER
        groups.contains(MANAGER_GROUP_ID) -> UserRole.MANAGER
        else -> UserRole.NONE
    }

    val userData = UserData(
        id = message.idDto?.toIntOrNull() ?: error("Не удалось получить ID пользователя"),
        name = message.nameDto ?: error("Не удалось получить имя пользователя"),
        lastName = message.lastNameDto,
        patronymic = message.secondNameDto,
        email = message.emailDto ?: error("Не удалось получить email пользователя"),
        accessLevel = accessLevel,
        role = userRole,
        buildings = message.buildingsDto?.mapNotNull { it?.toIntOrNull() } ?: emptyList(),
        services = message.servicesDto?.filterNotNull() ?: emptyList()
    )

    return userData
}