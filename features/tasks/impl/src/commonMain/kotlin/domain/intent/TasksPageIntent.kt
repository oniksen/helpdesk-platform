package domain.intent

sealed class TasksPageIntent {
    data class OpenDetailsPage(val id: Int) : TasksPageIntent()
}