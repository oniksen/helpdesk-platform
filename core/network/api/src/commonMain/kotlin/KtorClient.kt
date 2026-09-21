import io.ktor.client.HttpClient

interface KtorClient {
    fun baseInstance(): HttpClient
}