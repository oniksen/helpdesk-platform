package presentation.actions

data class TasksPageActions(
    val openDetailsPage: (id: Int) -> Unit,
    val changeLastVisibleIndex: (index: Int) -> Unit,
    val cancelJobs: () -> Unit,
)
