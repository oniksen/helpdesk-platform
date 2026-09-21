package user

data class UserData(
    val id: Int,
    val name: String,
    val lastName: String?,
    val patronymic: String?,
    val email: String,
    val accessLevel: AccessLevel,
    val role: UserRole,
    val buildings: List<Int>,
    val services: List<Int>,
)
