package data.repository

import KtorClient
import data.dto.TasksPageDto
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

private data class Filters(
    val buildings: List<Int>,
    val sort: Sort
)
private data class Sort(
    val dateLastChange: String,
)

internal class TasksRepository(
    client: KtorClient,
) {
    val client = client.baseInstance()

    suspend fun loadPage(
        page: Int,
        pageSize: Int = 10,
    ): TasksPageDto {
        // {{base_url}}/help-desk/v3/tasks/listing?page_size={{page_size}}&page={{page}}&filters={"buildings":[5]}&sort={"date_last_change":"desc"}

        val jsonFilters = Json.encodeToString(
            Filters(
                buildings = listOf(5),
                sort = Sort(
                    dateLastChange = "desc"
                )
            )
        )

        client.get {
            method = HttpMethod.Get
            url {
                protocol = URLProtocol.HTTPS
                parameters.apply {
                    append("page_size", pageSize.toString())
                    append("page", page.toString())
                    append("filters", jsonFilters)
                }
            }
        }
        val responseRaw = client.get("")
        val response = responseRaw.body<TasksPageDto>()

        return response
    }
}