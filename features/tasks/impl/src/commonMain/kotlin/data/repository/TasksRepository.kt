package data.repository

import KtorClient
import NetworkResult
import data.dto.TasksPageDto
import data.mapper.toDomain
import data.mapper.toDto
import domain.model.Filters
import domain.model.Sort
import domain.model.TasksPage
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import safeNetworkCall
import utils.toJsonString


internal class TasksRepository(
    client: KtorClient,
) {
    val client = client.baseInstance()

    /**
     * Загрузка указанной страницы с фильтрами и сортировкой.
     *
     * @throws NoTransformationFoundException Не найдена трансформация для запрашиваемого типа ответа.
     * @throws DoubleReceiveException Если тело ответа уже было использовано.
     *
     * */
    suspend fun loadPage(
        page: Int,
        pageSize: Int = 10,
        filters: Filters,
        sort: Sort,
    ): NetworkResult<TasksPage> {
        val jsonFiltersDto = filters.toDto().toJsonString()
        val jsonSort = sort.toJsonString()

        return safeNetworkCall {
            client.get {
                method = HttpMethod.Get
                url {
                    protocol = URLProtocol.HTTPS
                    host = "helpdesk.lpmti.ru"
                    path("help-desk","v3", "tasks", "listing")
                    parameters.apply {
                        append("page_size", pageSize.toString())
                        append("page", page.toString())
                        append("filters", jsonFiltersDto)
                        append("sort", jsonSort)
                    }
                }
            }.body<TasksPageDto>().toDomain()
        }
    }
}