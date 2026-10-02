import data.dto.manifest.v2.UpdateManifestDto
import data.mapper.toDomain
import domain.models.manifest.v2.UpdateManifest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Ветка обновлений, за которой следит веб-приложение.
 *
 * Пока prod не опубликован, приложение получает сборки из canary.
 * */
private const val WEB_TARGET_PATH = "channels.dev.canary.web"

actual class AppInstallStateImpl(
    client: KtorClient,
    private val versionProvider: AppVersionProvider,
) : AppInstallState {
    val client = client.baseInstance()
    actual override suspend fun hasNewVersion(): UpdateDecision {
        val currentVersion = versionProvider.provide()

        val networkResult = safeNetworkCall {
            client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "helpdesk.lpmti.ru"
                    path("helpdesk-app", "v2", "update-manifest-v4.json")
                }
            }.body<UpdateManifestDto>().toDomain()
        }

        return when (networkResult) {
            is NetworkResult.Error -> {
                UpdateDecision.ManifestError(networkResult.exception)
            }
            is NetworkResult.Success<UpdateManifest> -> {
                val target = networkResult.data.channels.dev.canary.web ?: return UpdateDecision.ManifestError(
                    AppException.ValidationError.MappingFailed(WEB_TARGET_PATH)
                )

                val remoteVersion = try {
                    target.version.normalizeFromTechnical()
                } catch (_: IllegalStateException) {
                    return UpdateDecision.ManifestError(
                        AppException.ValidationError.MappingFailed("$WEB_TARGET_PATH.version")
                    )
                }

                if (currentVersion >= remoteVersion) {
                    return UpdateDecision.UpToDate
                }

                UpdateDecision.UpdateAvailable(target.link)
            }
        }
    }
}
