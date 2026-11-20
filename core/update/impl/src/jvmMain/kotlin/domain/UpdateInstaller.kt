package domain

import DownloadStatus
import domain.model.AvailableUpdate
import domain.model.CurrentStageState
import domain.model.UpdateState
import domain.port.UpdateDownloader
import domain.port.ZipExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import platform.AppInstallDirResolver
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Полный пайплайн установки обновления JVM-приложения.
 *
 * Перенесён из проверенной реализации `UpdateManagerImpl` без изменения
 * последовательности этапов: загрузка → верификация (размер и SHA-256) →
 * проверка целостности zip → распаковка → замена файлов → очистка →
 * перезапуск.
 *
 * Замена файлов идёт через переименование занятого файла в `.bak` (обход
 * блокировки файлов на Windows) и пропускает встроенную JRE, иначе приложение
 * теряет рантайм.
 *
 * Любая ошибка этапа переводит состояние в `CurrentStageState.Error` и
 * прерывает установку: приложение продолжает работать в текущей версии.
 */
internal class UpdateInstaller(
    private val updateDownloader: UpdateDownloader,
    private val zipExtractor: ZipExtractor,
    private val appRestarter: AppRestarter,
    private val appInstallDirResolver: AppInstallDirResolver,
    private val platform: UpdatePlatform,
    private val downloadFileFactory: () -> File = { createDownloadFile(platform) },
) {
    /** Приёмник состояний текущей установки. */
    private var stateSink: (UpdateState) -> Unit = {}

    /** Последнее опубликованное состояние — нужно, чтобы не дублировать ошибку этапа. */
    private var lastEmittedState: UpdateState? = null

    /**
     * Запускает установку обновления.
     *
     * @param update Данные обновления из манифеста.
     * @param onState Приёмник состояний процесса.
     */
    suspend fun install(
        update: AvailableUpdate,
        onState: (UpdateState) -> Unit,
    ) {
        stateSink = onState

        val destinationFile = try {
            downloadFileFactory()
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            emit(UpdateState.Loading(CurrentStageState.Error(error.localizedMessage)))
            println("[UpdateInstaller] Не удалось подготовить файл обновления: ${error.message}")
            return
        }
        println("[UpdateInstaller] Загрузка обновления: $destinationFile")

        emit(UpdateState.Loading(CurrentStageState.Progress(0f)))

        updateDownloader.downloadWithProgress(
            url = update.url,
            destinationFilePath = destinationFile.path,
        )
            .catch { error ->
                if (error is CancellationException) throw error
                emit(UpdateState.Loading(CurrentStageState.Error(error.localizedMessage)))
                println("[UpdateInstaller] Ошибка загрузки обновления: ${error.message}")
            }
            .collect { status ->
                when (status) {
                    is DownloadStatus.Progress -> {
                        val progress = status.percentage * PERCENT_SCALE
                        emit(UpdateState.Loading(CurrentStageState.Progress(progress)))
                    }

                    is DownloadStatus.Success -> {
                        emit(UpdateState.Loading(CurrentStageState.Success))
                        delay(STEP_DELAY_MILLIS.milliseconds)
                        installArchive(File(status.filePath), update)
                    }
                }
            }
    }

    private suspend fun installArchive(file: File, update: AvailableUpdate) {
        val tempDir = File(
            System.getProperty("java.io.tmpdir"),
            "app_extracted_${System.currentTimeMillis()}",
        )

        try {
            val installDir = appInstallDirResolver.getAppInstallDir(this::class.java)
            verifyArchive(file, update.hash, update.size)
            checkArchiveIntegrity(file)
            unpackArchive(file, tempDir)
            replaceFiles(tempDir, installDir)
            cleanupAfterInstall(installDir, tempDir)
            restartApplication()
        } catch (e: CancellationException) {
            if (tempDir.exists()) tempDir.deleteRecursively()
            throw e
        } catch (error: Exception) {
            println("[UpdateInstaller] Установка обновления прервана: ${error.message}")
            if (tempDir.exists()) tempDir.deleteRecursively()
            if (!lastEmittedState.hasStageError()) {
                emit(UpdateState.Installation(CurrentStageState.Error(error.localizedMessage)))
            }
        }
    }

    /** Верификация обязательна: проверяются и размер, и SHA-256 скачанного архива. */
    private suspend fun verifyArchive(file: File, expectedHash: String, expectedSize: Long) {
        emit(UpdateState.Verification(CurrentStageState.Progress(0f)))

        val actualSize = file.length()
        if (actualSize != expectedSize) {
            val message = "Размер архива не совпал: ожидалось $expectedSize байт, получено $actualSize"
            emit(UpdateState.Verification(CurrentStageState.Error(message)))
            error(message)
        }

        val actualHash = file.computeSha256()
        emit(UpdateState.Verification(CurrentStageState.Progress(50f)))
        if (!actualHash.equals(expectedHash, ignoreCase = true)) {
            val message = "Хеш архива не совпал: ожидался $expectedHash, получен $actualHash"
            emit(UpdateState.Verification(CurrentStageState.Error(message)))
            error(message)
        }

        emit(UpdateState.Verification(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        emit(UpdateState.Verification(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun checkArchiveIntegrity(file: File) {
        emit(UpdateState.IntegrityChecking(CurrentStageState.Progress(0f)))

        if (!zipExtractor.verify(file)) {
            val message = "Архив обновления повреждён: проверка целостности zip не пройдена"
            emit(UpdateState.IntegrityChecking(CurrentStageState.Error(message)))
            error(message)
        }

        emit(UpdateState.IntegrityChecking(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        emit(UpdateState.IntegrityChecking(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun unpackArchive(file: File, tempDir: File) {
        emit(UpdateState.Unpacking(CurrentStageState.Progress(0f)))
        tempDir.mkdirs()

        if (!zipExtractor.extract(file, tempDir)) {
            val message = "Не удалось распаковать архив обновления"
            emit(UpdateState.Unpacking(CurrentStageState.Error(message)))
            error(message)
        }

        emit(UpdateState.Unpacking(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        emit(UpdateState.Unpacking(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun replaceFiles(tempDir: File, installDir: File) {
        emit(UpdateState.Installation(CurrentStageState.Progress(0f)))
        replaceFilesSafely(tempDir, installDir)
        emit(UpdateState.Installation(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        emit(UpdateState.Installation(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    /**
     * Удаляет `.bak`-файлы (иначе ломается кодовая подпись macOS), переподписывает
     * приложение и стирает временную директорию до перезапуска — `exitProcess`
     * не выполняет `finally`.
     */
    private suspend fun cleanupAfterInstall(installDir: File, tempDir: File) {
        emit(UpdateState.CleaningTemporaryFiles(CurrentStageState.Progress()))
        cleanupBackupFiles(installDir)
        reSignApplication()
        tempDir.deleteRecursively()
        emit(UpdateState.CleaningTemporaryFiles(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun restartApplication() {
        var secondsRemaining = RESTART_SECONDS

        while (secondsRemaining > 0) {
            emit(UpdateState.Restarting(secondsRemaining))
            secondsRemaining -= 1
            delay(1.seconds)
        }

        appRestarter.restart()
    }

    /**
     * Удаляет все `.bak`-файлы из целевой директории.
     * Необходимо для сохранения валидности кодовой подписи macOS.
     */
    internal fun cleanupBackupFiles(dir: File) {
        dir.walkTopDown().forEach { file ->
            if (file.name.endsWith(BACKUP_SUFFIX)) {
                file.delete()
            }
        }
    }

    /**
     * Переподписывает приложение ad-hoc подписью (macOS).
     * После замены файлов внутри `.app` бандла кодовая подпись становится
     * невалидной, и Gatekeeper блокирует запуск.
     */
    private fun reSignApplication() {
        if (platform != UpdatePlatform.MACOS) return

        val appBundle = appInstallDirResolver.getAppBundlePath(this::class.java) ?: return
        println("[UpdateInstaller] Переподпись приложения: ${appBundle.absolutePath}")

        try {
            val process = ProcessBuilder(
                "codesign",
                "--force",
                "--deep",
                "--sign",
                "-",
                appBundle.absolutePath,
            ).start()
            val exited = process.waitFor(CODESIGN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (exited && process.exitValue() == 0) {
                println("[UpdateInstaller] Переподпись завершена")
            } else {
                println("[UpdateInstaller] Предупреждение: codesign вернул код ${process.exitValue()}")
            }
        } catch (error: Exception) {
            println("[UpdateInstaller] Предупреждение: codesign не выполнен: ${error.message}")
        }
    }

    /**
     * Безопасно заменяет файлы в целевой директории файлами из исходной.
     *
     * Существующий файл сначала переименовывается в `.bak` — так обходится
     * блокировка занятых файлов на Windows, — затем на его место копируется новый.
     * Встроенная JRE (`runtime/`, `Contents/runtime/`) не заменяется.
     */
    internal suspend fun replaceFilesSafely(sourceDir: File, targetDir: File) {
        val files = sourceDir.walkTopDown()
        val fileToPercentage = files.count() / PERCENT_SCALE
        var progress = 0f

        files.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir)
            val pathString = relativePath.path.replace(File.separatorChar, '/')

            if (isBundledRuntime(pathString)) return@forEach

            val targetFile = File(targetDir, relativePath.path)

            if (sourceFile.isDirectory) {
                targetFile.mkdirs()
            } else {
                replaceFile(sourceFile, targetFile)
            }

            progress += fileToPercentage
            emit(UpdateState.Installation(CurrentStageState.Progress(progress)))
        }
    }

    private fun replaceFile(sourceFile: File, targetFile: File) {
        if (targetFile.exists()) {
            val backupFile = File("${targetFile.absolutePath}$BACKUP_SUFFIX")
            if (backupFile.exists()) backupFile.delete()
            targetFile.renameTo(backupFile)
        }

        sourceFile.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Сохраняем бит исполнимости (macOS: лаунчер приложения)
        if (sourceFile.canExecute()) {
            targetFile.setExecutable(true, false)
        }
    }

    /** Признак встроенной JRE: её бинарники нельзя заменять на работающем процессе. */
    private fun isBundledRuntime(path: String): Boolean = path == RUNTIME_DIR ||
        path.startsWith("$RUNTIME_DIR/") ||
        path == MACOS_RUNTIME_DIR ||
        path.startsWith("$MACOS_RUNTIME_DIR/")

    private fun emit(state: UpdateState) {
        lastEmittedState = state
        stateSink(state)
    }

    private companion object {
        const val RUNTIME_DIR = "runtime"
        const val MACOS_RUNTIME_DIR = "Contents/runtime"
        const val BACKUP_SUFFIX = ".bak"

        const val PERCENT_SCALE = 100f
        const val STEP_DELAY_MILLIS = 300L
        const val RESTART_SECONDS = 5
        const val CODESIGN_TIMEOUT_SECONDS = 30L
    }
}

private const val UPDATE_FILE_NAME = "helpdesk-update.zip"
private const val WINDOWS_UPDATES_DIR = "AppData/Roaming/Helpdesk/updates"
private const val MACOS_UPDATES_DIR = "Library/Application Support/Helpdesk/updates"
private const val OTHER_UPDATES_DIR = ".helpdesk/updates"

/** Каталог загрузки — тот же, что у desktop-приложения на соответствующей ОС. */
internal fun createDownloadFile(platform: UpdatePlatform?): File {
    val userHome = System.getProperty("user.home")
    val updatesDir = when (platform) {
        UpdatePlatform.WINDOWS -> File(userHome, WINDOWS_UPDATES_DIR)
        UpdatePlatform.MACOS -> File(userHome, MACOS_UPDATES_DIR)
        else -> File(userHome, OTHER_UPDATES_DIR)
    }
    updatesDir.mkdirs()
    return File(updatesDir, UPDATE_FILE_NAME)
}

/** Вычисляет SHA-256 хеш файла для верификации целостности скачанного архива. */
private fun File.computeSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")

    inputStream().buffered().use { input ->
        val buffer = ByteArray(SHA256_BUFFER_SIZE)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}

/** Размер буфера чтения при подсчёте хеша. */
private const val SHA256_BUFFER_SIZE = 8192
