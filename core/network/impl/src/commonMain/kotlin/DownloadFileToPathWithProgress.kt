import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

expect suspend fun downloadFileToPathWithProgress(
    httpClient: HttpClient,
    url: String,
    destinationFilePath: String,
): Flow<DownloadStatus>