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

internal class TasksRepositoryImpl(
    client: KtorClient,
) : TasksRepository {
    private val httpClient = client.baseInstance()

    override suspend fun loadPage(
        page: Int,
        pageSize: Int,
        filters: Filters,
        sort: Sort,
    ): NetworkResult<TasksPage> {
        val jsonFiltersDto = filters.toDto().toJsonString()
        val jsonSort = sort.toJsonString()

        return safeNetworkCall {
            httpClient.get {
                method = HttpMethod.Get
                url {
                    protocol = URLProtocol.HTTPS
                    host = "helpdesk.lpmti.ru"
                    path("help-desk", "v3", "tasks", "listing")
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
