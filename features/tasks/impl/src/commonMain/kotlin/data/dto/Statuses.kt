package data.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Statuses(
    @SerialName("assigned_users") val assignedUsersDto: List<Int?>? = null,
    @SerialName("completed_users") val completedUsersDto: List<Int?>? = null,
    @SerialName("in_progress_users") val inProgressUsersDto: List<Int?>? = null
)