package domain

import CurrentChannelProvider
import DownloadStatus
import domain.model.CurrentStageState
import domain.model.UpdateState
import domain.model.response.access.DevChannelAccess
import domain.model.response.action.ActionResponse
import domain.models.manifest.v2.UpdateManifest
import domain.port.UpdateDownloader
import domain.port.VersionActions
import domain.port.ZipExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import normalizeFromTechnical
import platform.AppInstallDirResolver
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Реализация [UpdateManager] для JVM-приложения.
 *
 * ## Цикл установки
 * [performUpdate] скачивает архив с прогрессом, после чего [installUpdate]
 * проходит этапы: верификация (размер и SHA-256) → проверка целостности zip →
 * распаковка → замена файлов → очистка → перезапуск.
 *
 * Замена файлов идёт через переименование занятого файла в `.bak` (обход блокировки
 * файлов на Windows) и пропускает встроенную JRE, иначе приложение теряет рантайм.
 *
 * Любая ошибка этапа переводит [updateState] в `Error(message)` и прерывает установку:
 * приложение продолжает работать в текущей версии.
 */
class UpdateManagerImpl(
    private val updateDownloader: UpdateDownloader,
    private val zipExtractor: ZipExtractor,
    private val appRestarter: AppRestarter,
    private val appInstallDirResolver: AppInstallDirResolver,
    private val versionActions: VersionActions,
    private val channelProvider: CurrentChannelProvider,
    private val platform: UpdatePlatform? = currentUpdatePlatform(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : UpdateManager {

    override val updateState: Flow<UpdateState>
        field = MutableStateFlow<UpdateState>(UpdateState.Idle(stage = CurrentStageState.Idle))

    override suspend fun loadManifest(currentSystemVersion: String): UpdateManifest? = runCatching {
        val manifest = updateDownloader.downloadUpdateManifest() ?: return@runCatching null
        val currentPlatform = platform ?: return@runCatching null
        val channel = channelProvider.provide() ?: UpdateChannel.PROD
        val target = manifest.targetFor(channel, currentPlatform) ?: return@runCatching null
        val availableVersion = target.version.normalizeFromTechnical()
        val installedVersion = currentSystemVersion.normalizeFromTechnical()
        if (availableVersion > installedVersion) manifest else null
    }.onFailure { error ->
        println("[UpdateManager] Проверка обновлений не выполнена: ${error.message}")
    }.getOrNull()

    override suspend fun hasNewVersion(currentSystemVersion: String, userEmail: String): Boolean {
        val channelsManifest = updateDownloader.downloadChannelsManifest() ?: return false
        val accessedChannels = channelsManifest.users[userEmail] ?: channelsManifest.default ?: return false

        accessedChannels.selected?.let { channel ->
            TODO()
        }
        TODO()
    }

    override fun performUpdate(
        updateUrl: String,
        expectedHash: String,
        expectedSize: Long,
    ) {
        val destinationFile = createDownloadFile()
        println("[UpdateManager] Загрузка обновления: $destinationFile")

        scope.launch {
            updateDownloader.downloadWithProgress(
                url = updateUrl,
                destinationFilePath = destinationFile.path,
            )
                .catch { error ->
                    updateState.emit(UpdateState.Loading(CurrentStageState.Error(error.localizedMessage)))
                    println("[UpdateManager] Ошибка загрузки обновления: ${error.message}")
                }
                .collect { status ->
                    when (status) {
                        is DownloadStatus.Progress -> {
                            val progress = status.percentage * PERCENT_SCALE
                            updateState.emit(UpdateState.Loading(CurrentStageState.Progress(progress)))
                        }

                        is DownloadStatus.Success -> {
                            updateState.emit(UpdateState.Loading(CurrentStageState.Success))
                            delay(STEP_DELAY_MILLIS.milliseconds)
                            installUpdate(File(status.filePath), expectedHash, expectedSize)
                        }
                    }
                }
        }
    }

    override suspend fun markAsStable(): ActionResponse? = versionActions.markAsStable()

    override suspend fun reject(reason: String): ActionResponse? = versionActions.reject(reason)

    override suspend fun publish(): ActionResponse? = versionActions.publish()

    override suspend fun devChannelAccess(): DevChannelAccess? = versionActions.devChannelAccess()

    /** Каталог загрузки — тот же, что у desktop-приложения на соответствующей ОС. */
    private fun createDownloadFile(): File {
        val userHome = System.getProperty("user.home")
        val updatesDir = when (platform) {
            UpdatePlatform.WINDOWS -> File(userHome, WINDOWS_UPDATES_DIR)
            UpdatePlatform.MACOS -> File(userHome, MACOS_UPDATES_DIR)
            else -> File(userHome, OTHER_UPDATES_DIR)
        }
        updatesDir.mkdirs()
        return File(updatesDir, UPDATE_FILE_NAME)
    }

    private suspend fun installUpdate(file: File, expectedHash: String, expectedSize: Long) {
        val tempDir = File(
            System.getProperty("java.io.tmpdir"),
            "app_extracted_${System.currentTimeMillis()}",
        )
        val installDir = appInstallDirResolver.getAppInstallDir(this::class.java)

        try {
            verifyArchive(file, expectedHash, expectedSize)
            checkArchiveIntegrity(file)
            unpackArchive(file, tempDir)
            replaceFiles(tempDir, installDir)
            cleanupAfterInstall(installDir, tempDir)
            restartApplication()
        } catch (error: Exception) {
            println("[UpdateManager] Установка обновления прервана: ${error.message}")
            if (tempDir.exists()) tempDir.deleteRecursively()
        }
    }

    /** Верификация обязательна: проверяются и размер, и SHA-256 скачанного архива. */
    private suspend fun verifyArchive(file: File, expectedHash: String, expectedSize: Long) {
        updateState.emit(UpdateState.Verification(CurrentStageState.Progress(0f)))

        val actualSize = file.length()
        if (actualSize != expectedSize) {
            val message = "Размер архива не совпал: ожидалось $expectedSize байт, получено $actualSize"
            updateState.emit(UpdateState.Verification(CurrentStageState.Error(message)))
            error(message)
        }

        val actualHash = file.computeSha256()
        updateState.emit(UpdateState.Verification(CurrentStageState.Progress(50f)))
        if (!actualHash.equals(expectedHash, ignoreCase = true)) {
            val message = "Хеш архива не совпал: ожидался $expectedHash, получен $actualHash"
            updateState.emit(UpdateState.Verification(CurrentStageState.Error(message)))
            error(message)
        }

        updateState.emit(UpdateState.Verification(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        updateState.emit(UpdateState.Verification(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun checkArchiveIntegrity(file: File) {
        updateState.emit(UpdateState.IntegrityChecking(CurrentStageState.Progress(0f)))

        if (!zipExtractor.verify(file)) {
            val message = "Архив обновления повреждён: проверка целостности zip не пройдена"
            updateState.emit(UpdateState.IntegrityChecking(CurrentStageState.Error(message)))
            error(message)
        }

        updateState.emit(UpdateState.IntegrityChecking(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        updateState.emit(UpdateState.IntegrityChecking(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun unpackArchive(file: File, tempDir: File) {
        updateState.emit(UpdateState.Unpacking(CurrentStageState.Progress(0f)))
        tempDir.mkdirs()

        if (!zipExtractor.extract(file, tempDir)) {
            val message = "Не удалось распаковать архив обновления"
            updateState.emit(UpdateState.Unpacking(CurrentStageState.Error(message)))
            error(message)
        }

        updateState.emit(UpdateState.Unpacking(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        updateState.emit(UpdateState.Unpacking(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun replaceFiles(tempDir: File, installDir: File) {
        updateState.emit(UpdateState.Installation(CurrentStageState.Progress(0f)))
        replaceFilesSafely(tempDir, installDir)
        updateState.emit(UpdateState.Installation(CurrentStageState.Progress(100f)))
        delay(STEP_DELAY_MILLIS.milliseconds)
        updateState.emit(UpdateState.Installation(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    /**
     * Удаляет `.bak`-файлы (иначе ломается кодовая подпись macOS), переподписывает
     * приложение и стирает временную директорию до перезапуска — `exitProcess`
     * не выполняет `finally`.
     */
    private suspend fun cleanupAfterInstall(installDir: File, tempDir: File) {
        updateState.emit(UpdateState.CleaningTemporaryFiles(CurrentStageState.Progress()))
        cleanupBackupFiles(installDir)
        reSignApplication()
        tempDir.deleteRecursively()
        updateState.emit(UpdateState.CleaningTemporaryFiles(CurrentStageState.Success))
        delay(STEP_DELAY_MILLIS.milliseconds)
    }

    private suspend fun restartApplication() {
        var secondsRemaining = RESTART_SECONDS

        while (secondsRemaining > 0) {
            updateState.emit(UpdateState.Restarting(secondsRemaining))
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
        println("[UpdateManager] Переподпись приложения: ${appBundle.absolutePath}")

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
                println("[UpdateManager] Переподпись завершена")
            } else {
                println("[UpdateManager] Предупреждение: codesign вернул код ${process.exitValue()}")
            }
        } catch (error: Exception) {
            println("[UpdateManager] Предупреждение: codesign не выполнен: ${error.message}")
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
            updateState.emit(UpdateState.Installation(CurrentStageState.Progress(progress)))
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

    private companion object {
        const val UPDATE_FILE_NAME = "helpdesk-update.zip"
        const val WINDOWS_UPDATES_DIR = "AppData/Roaming/Helpdesk/updates"
        const val MACOS_UPDATES_DIR = "Library/Application Support/Helpdesk/updates"
        const val OTHER_UPDATES_DIR = ".helpdesk/updates"

        const val RUNTIME_DIR = "runtime"
        const val MACOS_RUNTIME_DIR = "Contents/runtime"
        const val BACKUP_SUFFIX = ".bak"

        const val PERCENT_SCALE = 100f
        const val STEP_DELAY_MILLIS = 300L
        const val RESTART_SECONDS = 5
        const val CODESIGN_TIMEOUT_SECONDS = 30L
    }
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
