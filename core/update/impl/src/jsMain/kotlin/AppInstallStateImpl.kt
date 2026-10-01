import data.dto.manifest.v1.UpdateManifestDto
import data.mapper.toDomain
import domain.models.manifest.v1.UpdateManifest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

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
                    path("helpdesk-app","update-manifest-v3.json")
                }
            }.body<UpdateManifestDto>().toDomain()
        }

        return when (networkResult) {
            is NetworkResult.Error -> {
                UpdateDecision.ManifestError(networkResult.exception)
            }
            is NetworkResult.Success<UpdateManifest> -> {
                val prod = networkResult.data.channels.prod
                val remoteVersion = try {
                    prod.lastVersion.normalizeFromTechnical()
                } catch (_: IllegalStateException) {
                    return UpdateDecision.ManifestError(
                        AppException.ValidationError.MappingFailed("last_version")
                    )
                }

                if (currentVersion >= remoteVersion) {
                    return UpdateDecision.UpToDate
                }

                val web = prod.web ?: return UpdateDecision.ManifestError(
                    AppException.ValidationError.MappingFailed("web")
                )

                UpdateDecision.UpdateAvailable(web.link)
            }
        }
    }
}
