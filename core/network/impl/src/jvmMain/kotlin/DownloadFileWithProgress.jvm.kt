import DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.InternalIoApi
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import java.io.File
import java.io.IOException

/**
 * Потоковая загрузка файла на диск с эмиссией прогресса.
 *
 * Порт проверенной JVM-реализации донора: файл пишется через okio, прогресс
 * считается по Content-Length, неполная загрузка считается ошибкой.
 * */
@OptIn(InternalIoApi::class)
internal suspend fun downloadToPathWithProgress(
    httpClient: HttpClient,
    url: String,
    destinationFilePath: String,
): Flow<DownloadStatus> = flow {
    println("[Download] Начало загрузки: $url")
    println("[Download] Назначение: $destinationFilePath")

    val destinationFile = File(destinationFilePath)

    httpClient.prepareGet(url).execute { response ->
        val statusCode = response.status.value
        println("[Download] HTTP-статус: $statusCode")
        if (statusCode !in 200..299) {
            throw IOException("Загрузка не удалась: HTTP $statusCode")
        }

        val channel = response.bodyAsChannel()
        val totalBytes = response.contentLength() ?: -1L
        println("[Download] Content-Length: $totalBytes")

        var loadedBytes = 0L

        if (destinationFile.exists()) {
            println("[Download] Удаляю прежний файл (${destinationFile.length()} байт)")
            destinationFile.delete()
        }

        FileSystem.SYSTEM.sink(destinationFile.toOkioPath()).buffer().use { sink ->
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

                val bytesRead = packet.buffer.readByteArray()
                if (bytesRead.isEmpty()) break
                sink.write(bytesRead)

                loadedBytes += bytesRead.size
                if (totalBytes > 0) {
                    emit(DownloadStatus.Progress(loadedBytes.toFloat() / totalBytes))
                }
            }

            sink.flush()

            println("[Download] Загружено $loadedBytes из $totalBytes байт")
            println("[Download] Размер файла на диске: ${destinationFile.length()}")

            if (totalBytes > 0 && loadedBytes != totalBytes) {
                throw IOException(
                    "Загрузка неполная: ожидалось $totalBytes байт, получено $loadedBytes"
                )
            }
            emit(DownloadStatus.Success(destinationFilePath))
        }
    }
}

/** Размер буфера чтения из сетевого канала. */
private const val DEFAULT_BUFFER_SIZE = 1024 * 8
