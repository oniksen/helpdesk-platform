import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class KtorClientImpl(
    private val tokenProvider: Lazy<TokenProvider>,
    private val engine: HttpClientEngineFactory<HttpClientEngineConfig>
) : KtorClient {
    override fun baseInstance(): HttpClient = HttpClient(engine) {
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenProvider.value.provide() ?: run {
                        println("[DIAG] loadTokens: token=null")
                        return@loadTokens null
                    }
                    println("[DIAG] loadTokens: token=present")
                    BearerTokens(
                        accessToken = token,
                        refreshToken = null,
                    )
                }
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT
            requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private companion object {
        const val DEFAULT_CONNECT_TIMEOUT = 10_000L
        const val DEFAULT_REQUEST_TIMEOUT = 15_000L
    }
}