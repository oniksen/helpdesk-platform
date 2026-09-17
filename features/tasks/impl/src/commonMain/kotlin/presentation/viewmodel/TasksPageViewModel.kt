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
import domain.model.TasksPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import presentation.effect.TasksPageEffect
import presentation.state.TasksPageState
import kotlin.time.Duration.Companion.milliseconds

internal class TasksPageViewModel(
    private val tasksRepository: TasksRepository,
    private val navigator: AppNavigator
): BaseViewModel<TasksPageState, TasksPageEffect>(TasksPageState()) {
    /** Кэш самих объектов. */
    private val entityCache: MutableMap<Long, TaskModel> = HashMap()
    private val filterCache: MutableMap<Int, MutableList<Long>> = HashMap()
    private var firstVisibleIndex = 0

    init {
        loadPage(
            page = 1,
            filters = Filters(buildings = listOf(5)),
            sort = Sort(SortParam.DateLastChanged, Order.desc,)
        )
        scope.launch {
            while (isActive) {
                delay(5_000.milliseconds)
                val currentPage = (firstVisibleIndex / 10) + 1
                loadPage(page = currentPage, filters = Filters(buildings = listOf(5)), sort = Sort(SortParam.DateLastChanged, Order.desc,))
            }
        }
    }

    fun sendIntent(intent: TasksPageIntent) {
        when (intent) {
            is TasksPageIntent.OpenDetailsPage -> TODO()
            is TasksPageIntent.FirstVisibleIndexChanged -> { firstVisibleIndex = intent.index }
        }
    }

    private fun loadPage(page: Int, filters: Filters, sort: Sort) {
        scope.launch(Dispatchers.Default) {
            tasksRepository.loadPage(
                page = page,
                filters = filters,
                sort = sort,
            )
                .getResultOrSendEffect { error -> showError(error = error) }
                ?.let { result ->
                    onDataLoaded(filters, page, result)
                    val filterHash = filters.hashCode()
                    val actualListIds = filterCache[filterHash] ?: emptyList()
                    val actualEntities = actualListIds.mapNotNull { entityCache[it] }

                    updateState {
                        copy(
                            currentList = actualEntities,
                            hasNextPage = result.pagination.hasNext,
                        )
                    }
                }
        }
    }

    private fun showError(error: AppException) {
        updateEffect {
            TasksPageEffect.ShowSnackBar(
                message = error.message ?: "Unknown error",
            )
        }
    }

    /** Обновить данные в кэше. */
    private fun onDataLoaded(filters: Filters, page: Int, rawData: TasksPage) {
        val filtersHash = filters.hashCode()

        rawData.data.forEach {
            entityCache[it.id.toLong()] = TaskModel(
                id = it.id.toLong(),
                title = it.title,
            )
        }

        if (page == 1) {
            filterCache[filtersHash] = rawData.data.map { it.id.toLong() }.toMutableList()
        } else {
            val currentListIds = filterCache[filtersHash] ?: mutableListOf()
            val newPageIds = rawData.data.map { it.id.toLong() }

            // Merge
            currentListIds.removeAll(newPageIds)
            currentListIds.addAll(newPageIds)

            filterCache[filtersHash] = currentListIds
        }
    }
}