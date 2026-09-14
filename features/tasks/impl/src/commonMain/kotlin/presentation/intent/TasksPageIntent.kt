package presentation.intent

sealed class TasksPageIntent {
    data object OpenParkingPage : TasksPageIntent()
}