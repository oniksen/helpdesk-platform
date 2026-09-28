import io.ktor.client.request.*
import io.ktor.http.*

actual class AppInstallStateImpl(
    client: KtorClient,
) : AppInstallState {
    val client = client.baseInstance()
    actual override suspend fun hasNewVersion(): Boolean {
        val networkResult = safeNetworkCall {
            client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "helpdesk.lpmti.ru"
                    path("helpdesk-app","update-manifest-v3.json")
                }
            }
        }

        TODO("Not yet implemented")
    }
}