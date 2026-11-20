import DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * JVM-реализация [KtorClient].
 *
 * Настройки повторяют проверенный клиент донора: движок CIO, повторы запросов,
 * бесконечные таймауты чтения (для больших архивов обновления) и логирование
 * только POST-запросов.
 *
 * Клиент создаётся один раз и живёт столько же, сколько Koin-сингл.
 * */
class JvmKtorClient(
    private val tokenProvider: Lazy<TokenProvider>,
) : KtorClient {

    private val client = HttpClient(CIO) {
        expectSuccess = false

        install(Auth) {
            bearer {
                loadTokens {
                    // Fail-safe: на JVM граф авторизации ещё не собран, поэтому
                    // отсутствие токена или самого бина — не ошибка запроса.
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
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = Long.MAX_VALUE
            socketTimeoutMillis = Long.MAX_VALUE
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
            filter { request -> request.method == HttpMethod.Post }
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }

        defaultRequest {
            header("Accept", "application/json")
        }
    }

    override fun baseInstance(): HttpClient = client

    override suspend fun downloadFileWithProgress(
        url: String,
        destinationFilePath: String,
    ): Flow<DownloadStatus> = downloadToPathWithProgress(
        httpClient = client,
        url = url,
        destinationFilePath = destinationFilePath,
    )

    private companion object {
        const val RETRY_ATTEMPTS = 5
        const val CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}
