package presentation.state

import domain.model.TaskModel

data class TasksPageState(
    val loading: Boolean = false,
    val currentList: List<TaskModel> = emptyList(),
)