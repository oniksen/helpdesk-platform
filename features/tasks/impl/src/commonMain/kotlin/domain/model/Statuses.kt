package domain.model

data class Statuses(
    val assignedUsersDto: List<Int>,
    val completedUsersDto: List<Int>,
    val inProgressUsersDto: List<Int>
)
