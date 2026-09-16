package data.repository

import KtorClient
import data.dto.TasksPageDto
import data.mapper.toDto
import domain.model.Filters
import domain.model.Sort
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import utils.toJsonString


internal class TasksRepository(
    client: KtorClient,
) {
    val client = client.baseInstance()

    suspend fun loadPage(
        page: Int,
        pageSize: Int = 10,
        filters: Filters,
        sort: Sort,
    ): TasksPageDto {
        // {{base_url}}/help-desk/v3/tasks/listing?page_size={{page_size}}&page={{page}}&filters={"buildings":[5]}&sort={"date_last_change":"desc"}


        // https://helpdesk.lpmti.ru/help-desk/v3/tasks/listing?page_size=10&page=1&filters={"buildings":[5]}&sort={"date_last_changed":"desc"}
        val jsonFiltersDto = filters.toDto().toJsonString()
        val jsonSort = sort.toJsonString()

        val responseRaw = client.get {
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
        }
        val response = responseRaw.body<TasksPageDto>()

        return response
    }
}