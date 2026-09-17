package presentation.actions

data class TasksPageActions(
    val openDetailsPage: (id: Int) -> Unit,
    val changeFirstVisibleIndex: (index: Int) -> Unit,
)
