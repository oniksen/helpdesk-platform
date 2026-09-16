package presentation.viewmodel

import AppNavigator
import data.repository.TasksRepository
import domain.intent.TasksPageIntent
import domain.model.Filters
import domain.model.Order
import domain.model.Sort
import domain.model.SortParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.effect.TasksPageEffect
import presentation.state.TasksPageState

internal class TasksPageViewModel(
    private val tasksRepository: TasksRepository,
    private val navigator: AppNavigator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val uiState: StateFlow<TasksPageState>
        field = MutableStateFlow(TasksPageState())
    val effect: SharedFlow<TasksPageEffect>
        field = MutableSharedFlow<TasksPageEffect>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    init {
        scope.launch(Dispatchers.Default) {
            val page = tasksRepository.loadPage(
                page = 1,
                filters = Filters(
                    buildings = listOf(5)
                ),
                sort = Sort(
                    param = SortParam.DateLastChanged,
                    order = Order.desc,
                )
            )

            println(page)
        }
    }

    fun sendIntent(intent: TasksPageIntent) {
        when (intent) {
            is TasksPageIntent.OpenDetailsPage -> TODO()
        }
    }

    private fun updateState(block: TasksPageState.() -> TasksPageState) {
        uiState.update { block.invoke(uiState.value) }
    }
}