package presentation.viewmodel

import AppException
import AppNavigator
import BaseViewModel
import data.repository.TasksRepository
import domain.intent.TasksPageIntent
import domain.model.Filters
import domain.model.Order
import domain.model.Sort
import domain.model.SortParam
import domain.model.TaskModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import presentation.effect.TasksPageEffect
import presentation.state.TasksPageState

internal class TasksPageViewModel(
    private val tasksRepository: TasksRepository,
    private val navigator: AppNavigator
): BaseViewModel<TasksPageState, TasksPageEffect>(TasksPageState()) {
    init {
        scope.launch(Dispatchers.Default) {
            tasksRepository.loadPage(
                page = 1,
                filters = Filters(
                    buildings = listOf(5)
                ),
                sort = Sort(
                    param = SortParam.DateLastChanged,
                    order = Order.desc,
                )
            )
                .getResultOrSendEffect(
                    callableError = { error ->
                        showError(error = error)
                    }
                )
                ?.let { result ->
                    updateState {
                        copy(
                            currentList = result.data.map { raw -> TaskModel(raw.id.toLong(), raw.title) }
                        )
                    }
                    println(result)
                }
        }
    }

    fun sendIntent(intent: TasksPageIntent) {
        when (intent) {
            is TasksPageIntent.OpenDetailsPage -> TODO()
        }
    }

    private fun showError(error: AppException) {
        updateEffect {
            TasksPageEffect.ShowSnackBar(
                message = error.message ?: "Unknown error",
            )
        }
    }
}