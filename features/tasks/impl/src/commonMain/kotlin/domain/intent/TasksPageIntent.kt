package domain.intent

sealed class TasksPageIntent {
    data class FirstVisibleIndexChanged(val index: Int) : TasksPageIntent()
    data class OpenDetailsPage(val id: Int) : TasksPageIntent()
}