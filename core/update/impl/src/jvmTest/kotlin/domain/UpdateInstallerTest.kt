package domain

import DownloadStatus
import domain.model.AvailableUpdate
import domain.model.CurrentStageState
import domain.model.UpdateChannel
import domain.model.UpdateState
import domain.port.UpdateDownloader
import domain.port.ZipExtractor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import normalizeFromTechnical
import platform.AppInstallDirResolver
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Пайплайн установки и файловые операции, перенесённые из старого
 * `UpdateManagerImpl` без изменения поведения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateInstallerTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("update-installer-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun `keeps previous version as backup while replacing files`() = runBlocking {
        val sourceDir = createDir("extracted")
        val installDir = createDir("install")
        createFile(sourceDir, "app/main.jar", NEW_CONTENT)
        val installedFile = createFile(installDir, "app/main.jar", OLD_CONTENT)

        createInstaller().replaceFilesSafely(sourceDir, installDir)

        assertEquals(NEW_CONTENT, installedFile.readText(), "Файл приложения не заменён новой версией")
        assertEquals(
            OLD_CONTENT,
            File(installDir, "app/main.jar$BACKUP_SUFFIX").readText(),
            "Предыдущая версия файла не сохранена рядом",
        )
    }

    @Test
    fun `keeps bundled runtime untouched`() = runBlocking {
        RUNTIME_PATHS.forEach { path ->
            val sourceDir = createDir("extracted-${path.hashCode()}")
            val installDir = createDir("install-${path.hashCode()}")
            createFile(sourceDir, path, NEW_CONTENT)
            val installedFile = createFile(installDir, path, OLD_CONTENT)

            createInstaller().replaceFilesSafely(sourceDir, installDir)

            assertEquals(
                OLD_CONTENT,
                installedFile.readText(),
                "Встроенный рантайм '$path' заменён — приложение потеряет JRE",
            )
        }
    }

    @Test
    fun `removes backup files after installation`() {
        val installDir = createDir("install")
        val workingFile = createFile(installDir, "app/main.jar", NEW_CONTENT)
        createFile(installDir, "app/main.jar$BACKUP_SUFFIX", OLD_CONTENT)
        createFile(installDir, "Contents/app/lib.jar$BACKUP_SUFFIX", OLD_CONTENT)

        createInstaller().cleanupBackupFiles(installDir)

        assertFalse(
            File(installDir, "app/main.jar$BACKUP_SUFFIX").exists(),
            "Резервная копия верхнего уровня не удалена",
        )
        assertFalse(
            File(installDir, "Contents/app/lib.jar$BACKUP_SUFFIX").exists(),
            "Вложенная резервная копия не удалена",
        )
        assertTrue(workingFile.exists(), "Рабочий файл удалён вместе с резервными копиями")
    }

    @Test
    fun `emits download verification integrity unpacking installation cleaning and restart states`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val update = updateFor(updateFile)
        val states = mutableListOf<UpdateState>()
        val restarter = RecordingRestarter()
        val installer = createInstaller(
            downloader = FakeDownloader(flowOf(DownloadStatus.Success(updateFile.path))),
            restarter = restarter,
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        installer.install(update) { states += it }

        assertTrue(
            states.any { it is UpdateState.Loading && it.stage is CurrentStageState.Success },
            "Загрузка не завершилась успехом",
        )
        assertTrue(
            states.any { it is UpdateState.Verification && it.stage is CurrentStageState.Success },
            "Верификация не завершилась успехом",
        )
        assertTrue(
            states.any { it is UpdateState.IntegrityChecking && it.stage is CurrentStageState.Success },
            "Проверка целостности не завершилась успехом",
        )
        assertTrue(
            states.any { it is UpdateState.Unpacking && it.stage is CurrentStageState.Success },
            "Распаковка не завершилась успехом",
        )
        assertTrue(
            states.any { it is UpdateState.Installation && it.stage is CurrentStageState.Success },
            "Установка не завершилась успехом",
        )
        assertTrue(
            states.any { it is UpdateState.CleaningTemporaryFiles && it.stage is CurrentStageState.Success },
            "Очистка не завершилась успехом",
        )
        assertTrue(
            states.any { it == UpdateState.Restarting(5) },
            "Отсчёт до перезапуска не начался с 5 секунд",
        )
        assertTrue(
            states.any { it == UpdateState.Restarting(1) },
            "Отсчёт до перезапуска не дошёл до 1 секунды",
        )
        assertEquals(1, restarter.calls, "Приложение должно быть перезапущено ровно один раз")
    }

    @Test
    fun `emits verification error when size does not match`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val update = updateFor(updateFile).copy(size = updateFile.length() + 1)
        val states = mutableListOf<UpdateState>()
        val restarter = RecordingRestarter()
        val installer = createInstaller(
            downloader = FakeDownloader(flowOf(DownloadStatus.Success(updateFile.path))),
            restarter = restarter,
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        installer.install(update) { states += it }

        assertTrue(
            states.any {
                it is UpdateState.Verification && it.stage is CurrentStageState.Error
            },
            "Неверный размер не привёл к ошибке верификации",
        )
        assertEquals(0, restarter.calls, "При ошибке верификации перезапуска быть не должно")
    }

    @Test
    fun `emits verification error when hash does not match`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val update = updateFor(updateFile).copy(hash = "bad-hash")
        val states = mutableListOf<UpdateState>()
        val restarter = RecordingRestarter()
        val installer = createInstaller(
            downloader = FakeDownloader(flowOf(DownloadStatus.Success(updateFile.path))),
            restarter = restarter,
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        installer.install(update) { states += it }

        assertTrue(
            states.any {
                it is UpdateState.Verification && it.stage is CurrentStageState.Error
            },
            "Неверный хеш не привёл к ошибке верификации",
        )
        assertEquals(0, restarter.calls, "При ошибке верификации перезапуска быть не должно")
    }

    @Test
    fun `emits integrity error when zip is corrupted`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val states = mutableListOf<UpdateState>()
        val restarter = RecordingRestarter()
        val installer = createInstaller(
            downloader = FakeDownloader(flowOf(DownloadStatus.Success(updateFile.path))),
            extractor = FakeZipExtractor(verifyResult = false),
            restarter = restarter,
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        installer.install(updateFor(updateFile)) { states += it }

        assertTrue(
            states.any {
                it is UpdateState.IntegrityChecking && it.stage is CurrentStageState.Error
            },
            "Повреждённый архив не привёл к ошибке целостности",
        )
        assertEquals(0, restarter.calls, "При ошибке целостности перезапуска быть не должно")
    }

    @Test
    fun `emits unpacking error when extraction fails`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val states = mutableListOf<UpdateState>()
        val restarter = RecordingRestarter()
        val installer = createInstaller(
            downloader = FakeDownloader(flowOf(DownloadStatus.Success(updateFile.path))),
            extractor = FakeZipExtractor(extractResult = false),
            restarter = restarter,
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        installer.install(updateFor(updateFile)) { states += it }

        assertTrue(
            states.any {
                it is UpdateState.Unpacking && it.stage is CurrentStageState.Error
            },
            "Ошибка распаковки не попала в состояние",
        )
        assertEquals(0, restarter.calls, "При ошибке распаковки перезапуска быть не должно")
    }

    @Test
    fun `propagates cancellation instead of reporting an error`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val states = mutableListOf<UpdateState>()
        val installer = createInstaller(
            downloader = FakeDownloader(
                flow {
                    emit(DownloadStatus.Progress(0.5f))
                    awaitCancellation()
                },
            ),
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        val job = launch {
            installer.install(updateFor(updateFile)) { states += it }
        }
        runCurrent()
        job.cancelAndJoin()

        assertTrue(job.isCancelled, "Установка должна быть отменена")
        assertFalse(
            states.any {
                it is UpdateState.Loading && it.stage is CurrentStageState.Error
            },
            "Отмена не должна превращаться в ошибку загрузки",
        )
    }

    @Test
    fun `propagates cancellation during archive integrity check`() = runTest {
        val updateFile = createFile(workDir, "update.zip", NEW_CONTENT)
        val gate = CompletableDeferred<Unit>()
        val states = mutableListOf<UpdateState>()
        val installer = createInstaller(
            downloader = FakeDownloader(flowOf(DownloadStatus.Success(updateFile.path))),
            extractor = SuspendingVerifyExtractor(gate),
            downloadFile = updateFile,
            installDir = createDir("install"),
        )

        val job = launch {
            installer.install(updateFor(updateFile)) { states += it }
        }
        testScheduler.advanceUntilIdle()
        job.cancelAndJoin()

        assertTrue(job.isCancelled, "Установка должна быть отменена")
        assertFalse(
            states.any {
                it is UpdateState.IntegrityChecking && it.stage is CurrentStageState.Error
            },
            "Отмена не должна превращаться в ошибку целостности",
        )
    }

    private fun createInstaller(
        downloader: UpdateDownloader = FakeDownloader(),
        extractor: ZipExtractor = FakeZipExtractor(),
        restarter: AppRestarter = RecordingRestarter(),
        downloadFile: File = File(workDir, "update.zip"),
        installDir: File = createDir("install"),
    ): UpdateInstaller = UpdateInstaller(
        updateDownloader = downloader,
        zipExtractor = extractor,
        appRestarter = restarter,
        appInstallDirResolver = object : AppInstallDirResolver {
            override fun getAppInstallDir(referenceClass: Class<*>): File = installDir

            override fun getAppBundlePath(referenceClass: Class<*>): File? = null
        },
        platform = UpdatePlatform.WINDOWS,
        downloadFileFactory = { downloadFile },
    )

    private fun updateFor(file: File): AvailableUpdate {
        val bytes = file.readBytes()
        return AvailableUpdate(
            version = "0.1.1001".normalizeFromTechnical(),
            channel = UpdateChannel.PROD,
            url = "https://example.com/update.zip",
            hash = bytes.sha256(),
            size = bytes.size.toLong(),
        )
    }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun createDir(name: String): File = File(workDir, name).apply { mkdirs() }

    private fun createFile(directory: File, path: String, content: String): File =
        File(directory, path).apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    private class FakeDownloader(
        private val flow: Flow<DownloadStatus> = emptyFlow(),
    ) : UpdateDownloader {
        override suspend fun downloadUpdateManifest() = error("Манифест в тестах установки не используется")

        override suspend fun downloadChannelsManifest() = error("Каналы в тестах установки не используются")

        override suspend fun downloadWithProgress(
            url: String,
            destinationFilePath: String,
        ): Flow<DownloadStatus> = flow
    }

    private class FakeZipExtractor(
        private val verifyResult: Boolean = true,
        private val extractResult: Boolean = true,
    ) : ZipExtractor {
        override suspend fun verify(zipFile: File): Boolean = verifyResult

        override suspend fun extract(zipFile: File, destinationDir: File): Boolean = extractResult
    }

    private class SuspendingVerifyExtractor(
        private val gate: CompletableDeferred<Unit>,
    ) : ZipExtractor {
        override suspend fun verify(zipFile: File): Boolean {
            gate.await()
            return true
        }

        override suspend fun extract(zipFile: File, destinationDir: File): Boolean = true
    }

    private class RecordingRestarter : AppRestarter {
        var calls: Int = 0
            private set

        override fun restart() {
            calls += 1
        }
    }

    private companion object {
        const val OLD_CONTENT = "old"
        const val NEW_CONTENT = "new"
        const val BACKUP_SUFFIX = ".bak"

        /** Пути встроенного рантайма, которые нельзя заменять на работающем процессе. */
        val RUNTIME_PATHS = listOf(
            "runtime/bin/java",
            "Contents/runtime/bin/java",
        )
    }
}
