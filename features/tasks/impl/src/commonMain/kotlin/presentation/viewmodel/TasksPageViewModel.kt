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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import presentation.effect.TasksPageEffect
import presentation.state.TasksPageState
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class TasksPageViewModel(
    private val tasksRepository: TasksRepository,
    private val navigator: AppNavigator
): BaseViewModel<TasksPageState, TasksPageEffect>(TasksPageState()) {
    /** Кэш самих объектов. */
    private val entityCache: MutableMap<Long, TaskModel> = HashMap()
    private val filterCache: MutableMap<Int, MutableList<Long>> = HashMap()
    private val _currentPage = MutableStateFlow(1)

    init {
        scope.launch {
            _currentPage.flatMapLatest { page ->
                flow {
                    while(currentCoroutineContext().isActive) {
                        emit(page)
                        delay(5_000.milliseconds)
                    }
                }
            }.collect { page ->
                loadPage(
                    page = page,
                    filters = Filters(buildings = listOf(5)),
                    sort = Sort(SortParam.DateLastChanged, Order.desc)
                )
            }
        }
    }

    fun sendIntent(intent: TasksPageIntent) {
        when (intent) {
            is TasksPageIntent.OpenDetailsPage -> TODO()
            is TasksPageIntent.FirstVisibleIndexChanged -> emitNewPage(intent.index)
            TasksPageIntent.CancelJobs -> cancelScope()
        }
    }

    private fun emitNewPage(index: Int) {
        // Если индекс на текущей странице < порогового значения, не должны подгружать следующую страницу. Иначе должны.
        val nextPageIndex = if (index % 10 < 3) 0 else 1
        val currentPage = index / 10 + 1 + nextPageIndex

        _currentPage.value = currentPage
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

    private fun cancelScope() {
        scope.cancel()
    }
}