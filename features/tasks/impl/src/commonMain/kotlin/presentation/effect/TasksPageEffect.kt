package presentation.effect

sealed class TasksPageEffect {
    data class ShowSnackBar(val message: String) : TasksPageEffect()
}