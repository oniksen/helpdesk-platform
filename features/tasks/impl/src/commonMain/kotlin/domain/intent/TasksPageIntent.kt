package domain.intent

sealed class TasksPageIntent {
    data class LastVisibleIndexChanged(val index: Int) : TasksPageIntent()
    data class OpenDetailsPage(val id: Int) : TasksPageIntent()
    data object CancelJobs : TasksPageIntent()
}