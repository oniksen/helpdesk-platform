package data.repository

import NetworkResult
import domain.model.Filters
import domain.model.Sort
import domain.model.TasksPage

/** Контракт постраничной загрузки списка задач. */
internal interface TasksRepository {

    /**
     * Загрузка указанной страницы с фильтрами и сортировкой.
     *
     * @throws NoTransformationFoundException Не найдена трансформация для запрашиваемого типа ответа.
     * @throws DoubleReceiveException Если тело ответа уже было использовано.
     */
    suspend fun loadPage(
        page: Int,
        pageSize: Int = 10,
        filters: Filters,
        sort: Sort,
    ): NetworkResult<TasksPage>
}
