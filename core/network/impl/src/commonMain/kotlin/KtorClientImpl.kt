import `helpdesk-platform`.config.BuildKonfig
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

internal class KtorClientImpl(
    private val tokenProvider: Lazy<TokenProvider>,
    private val engine: HttpClientEngineFactory<HttpClientEngineConfig>,
) : KtorClient {
    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(Auth) {
                bearer {
                    loadTokens {
                        val token = runCatching { tokenProvider.value.provide() }.getOrNull()
                            ?: return@loadTokens null
                        BearerTokens(
                            accessToken = token,
                            refreshToken = null,
                        )
                    }
                }
            }

            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = RETRY_ATTEMPTS)
                retryOnException(maxRetries = RETRY_ATTEMPTS, retryOnTimeout = true)
                exponentialDelay()
            }

            install(HttpTimeout) {
                connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT
                requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT
                socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT
            }

            install(Logging) {
                logger = Logger.SIMPLE
                level = if (BuildKonfig.NETWORK_DEBUG_LOGGING) LogLevel.ALL else LogLevel.NONE
                filter { request -> request.method == HttpMethod.Post }
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }

            defaultRequest {
                header("Accept", "application/json")
            }
        }
    }

    override fun baseInstance(): HttpClient = client

    override suspend fun downloadFileWithProgress(
        url: String,
        destinationFilePath: String,
    ): Flow<DownloadStatus> = downloadFileToPathWithProgress(
        httpClient = baseInstance(),
        url = url,
        destinationFilePath = destinationFilePath,
    )

    private companion object {
        const val DEFAULT_CONNECT_TIMEOUT = 10_000L
        const val DEFAULT_REQUEST_TIMEOUT = Long.MAX_VALUE
        const val DEFAULT_SOCKET_TIMEOUT = Long.MAX_VALUE
        const val RETRY_ATTEMPTS = 5
    }
}