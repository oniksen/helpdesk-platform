package data.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TasksPageDto(
    @SerialName("data") val dataDto: List<TaskDataDto?>? = null,
    @SerialName("pagination") val paginationDto: PaginationDto? = null
)