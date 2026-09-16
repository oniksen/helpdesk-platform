package domain.model

data class TasksPage(
    val data: List<TaskData>,
    val paginationDto: Pagination,
)