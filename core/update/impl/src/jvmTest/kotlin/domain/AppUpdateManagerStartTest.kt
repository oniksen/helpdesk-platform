package domain

import AppVersion
import AppVersionProvider
import DownloadStatus
import data.impl.ChannelAccessStoreImpl
import domain.model.CheckUpdateState
import domain.model.CurrentStageState
import domain.model.UpdateState
import domain.models.channels.ChannelAccess
import domain.models.channels.ChannelsFile
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest
import domain.port.UpdateDownloader
import domain.port.ZipExtractor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Запуск установки: fire-and-forget, защита от повторного запуска и
 * возможность повтора после ошибки.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateManagerStartTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("update-manager-start-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun `does nothing when no update is available`() = runTest {
        val updateFile = createFile(workDir, "update.zip", UPDATE_CONTENT)
        val target = targetFor(updateFile).copy(version = INSTALLED_VERSION)
        val downloader = FakeUpdateDownloader(
            manifest = manifestWithWindowsTarget(target),
            downloadFlow = flowOf(DownloadStatus.Success(updateFile.path)),
        )
        val restarter = RecordingRestarter()
        val manager = createManager(downloader, updateFile, restarter, scope = CoroutineScope(StandardTestDispatcher(testScheduler)))

        manager.checkUpdate()

        assertEquals(
            CheckUpdateState.IsUpToDate,
            manager.checkUpdateState.value,
            "Обновление не должно быть доступно",
        )

        manager.startUpdate()
        testScheduler.advanceUntilIdle()

        assertEquals(0, restarter.calls, "Установка не должна запускаться без обновления")
        assertEquals(UpdateState.Idle, manager.updateState.value, "Состояние установки должно остаться Idle")
    }

    @Test
    fun `runs the pipeline and restarts the app`() = runTest {
        val updateFile = createFile(workDir, "update.zip", UPDATE_CONTENT)
        val target = targetFor(updateFile)
        val downloader = FakeUpdateDownloader(
            manifest = manifestWithWindowsTarget(target),
            downloadFlow = flowOf(DownloadStatus.Success(updateFile.path)),
        )
        val restarter = RecordingRestarter()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val manager = createManager(downloader, updateFile, restarter, scope = scope)

        manager.checkUpdate()
        assertIs<CheckUpdateState.Available>(
            manager.checkUpdateState.value,
            "Обновление должно быть доступно",
        )

        manager.startUpdate()
        testScheduler.advanceUntilIdle()

        assertEquals(1, restarter.calls, "Приложение должно быть перезапущено")
        assertIs<UpdateState.Restarting>(
            manager.updateState.value,
            "Последним состоянием должен быть отсчёт до перезапуска",
        )
    }

    @Test
    fun `ignores repeated start while update is running`() = runTest {
        val updateFile = createFile(workDir, "update.zip", UPDATE_CONTENT)
        val target = targetFor(updateFile)
        val gate = CompletableDeferred<Unit>()
        val extractor = SuspendingZipExtractor(gate)
        val downloader = FakeUpdateDownloader(
            manifest = manifestWithWindowsTarget(target),
            downloadFlow = flowOf(DownloadStatus.Success(updateFile.path)),
        )
        val restarter = RecordingRestarter()
        val manager = createManager(
            downloader = downloader,
            downloadFile = updateFile,
            restarter = restarter,
            extractor = extractor,
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        )

        manager.checkUpdate()
        manager.startUpdate()
        runCurrent()
        manager.startUpdate()
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals(
            1,
            restarter.calls,
            "Повторный запуск не должен создавать вторую установку",
        )
    }

    @Test
    fun `allows retry after a failed install`() = runTest {
        val updateFile = createFile(workDir, "update.zip", UPDATE_CONTENT)
        val target = targetFor(updateFile)
        val extractor = ToggleZipExtractor(verifyResult = false)
        val downloader = FakeUpdateDownloader(
            manifest = manifestWithWindowsTarget(target),
            downloadFlow = flowOf(DownloadStatus.Success(updateFile.path)),
        )
        val restarter = RecordingRestarter()
        val manager = createManager(
            downloader = downloader,
            downloadFile = updateFile,
            restarter = restarter,
            extractor = extractor,
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        )

        manager.checkUpdate()
        manager.startUpdate()
        testScheduler.advanceUntilIdle()

        assertEquals(0, restarter.calls, "При ошибке целостности перезапуска быть не должно")
        assertTrue(
            manager.updateState.value is UpdateState.IntegrityChecking,
            "Должна быть ошибка целостности",
        )

        extractor.verifyResult = true
        manager.startUpdate()
        testScheduler.advanceUntilIdle()

        assertEquals(
            1,
            restarter.calls,
            "Повторный запуск после ошибки должен установить обновление",
        )
    }

    @Test
    fun `reports install failure when download file cannot be created`() = runTest {
        val updateFile = createFile(workDir, "update.zip", UPDATE_CONTENT)
        val target = targetFor(updateFile)
        val downloader = FakeUpdateDownloader(
            manifest = manifestWithWindowsTarget(target),
            downloadFlow = flowOf(DownloadStatus.Success(updateFile.path)),
        )
        val restarter = RecordingRestarter()
        val manager = createManager(
            downloader = downloader,
            downloadFile = updateFile,
            restarter = restarter,
            downloadFileFactory = { error("cannot create file") },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        )

        manager.checkUpdate()
        manager.startUpdate()
        testScheduler.advanceUntilIdle()

        val state = assertIs<UpdateState.Loading>(
            manager.updateState.value,
            "Ошибка подготовки файла должна быть видна в состоянии",
        )
        assertIs<CurrentStageState.Error>(
            state.stage,
            "Этап загрузки должен содержать ошибку",
        )
        assertEquals(0, restarter.calls, "При ошибке подготовки файла перезапуска быть не должно")
    }

    @Test
    fun `reports install failure when install dir cannot be resolved`() = runTest {
        val updateFile = createFile(workDir, "update.zip", UPDATE_CONTENT)
        val target = targetFor(updateFile)
        val downloader = FakeUpdateDownloader(
            manifest = manifestWithWindowsTarget(target),
            downloadFlow = flowOf(DownloadStatus.Success(updateFile.path)),
        )
        val restarter = RecordingRestarter()
        val manager = createManager(
            downloader = downloader,
            downloadFile = updateFile,
            restarter = restarter,
            installDirResolver = object : AppInstallDirResolver {
                override fun getAppInstallDir(referenceClass: Class<*>): File =
                    error("cannot resolve install dir")

                override fun getAppBundlePath(referenceClass: Class<*>): File? = null
            },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        )

        manager.checkUpdate()
        manager.startUpdate()
        testScheduler.advanceUntilIdle()

        val state = assertIs<UpdateState.Installation>(
            manager.updateState.value,
            "Ошибка директории установки должна быть видна в состоянии",
        )
        assertIs<CurrentStageState.Error>(
            state.stage,
            "Этап установки должен содержать ошибку",
        )
        assertEquals(0, restarter.calls, "При ошибке директории установки перезапуска быть не должно")
    }

    private fun createManager(
        downloader: UpdateDownloader,
        downloadFile: File,
        restarter: AppRestarter,
        extractor: ZipExtractor = NoOpZipExtractor,
        installDir: File = createDir("install"),
        installDirResolver: AppInstallDirResolver = object : AppInstallDirResolver {
            override fun getAppInstallDir(referenceClass: Class<*>): File = installDir

            override fun getAppBundlePath(referenceClass: Class<*>): File? = null
        },
        downloadFileFactory: () -> File = { downloadFile },
        scope: CoroutineScope,
    ): AppUpdateManagerImpl = AppUpdateManagerImpl(
        updateDownloader = downloader,
        zipExtractor = extractor,
        appRestarter = restarter,
        appInstallDirResolver = installDirResolver,
        channelAccessStore = ChannelAccessStoreImpl(),
        versionProvider = FakeVersionProvider(INSTALLED_VERSION.normalizeFromTechnical()),
        platform = UpdatePlatform.WINDOWS,
        scope = scope,
        downloadFileFactory = downloadFileFactory,
    )

    private fun manifestWithWindowsTarget(target: TargetData): UpdateManifest =
        ManifestFixtures.manifest(
            prod = ManifestFixtures.releaseWith(windows = target),
        )

    private fun targetFor(file: File): TargetData {
        val bytes = file.readBytes()
        return TargetData(
            version = "0.1.1001",
            patchNote = emptyList(),
            tags = emptyList(),
            date = "2026-11-19T00:00:00",
            link = "https://example.com/update.zip",
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

    private class FakeVersionProvider(
        private val version: AppVersion,
    ) : AppVersionProvider {
        override fun provide(): AppVersion = version
    }

    private class FakeUpdateDownloader(
        private val manifest: UpdateManifest,
        private val downloadFlow: Flow<DownloadStatus>,
    ) : UpdateDownloader {
        override suspend fun downloadUpdateManifest(): UpdateManifest = manifest

        override suspend fun downloadChannelsManifest(): ChannelsFile =
            ChannelsFile(
                default = ChannelAccess(allowed = emptyList(), selected = null),
                users = emptyMap(),
            )

        override suspend fun downloadWithProgress(
            url: String,
            destinationFilePath: String,
        ): Flow<DownloadStatus> = downloadFlow
    }

    private object NoOpZipExtractor : ZipExtractor {
        override suspend fun verify(zipFile: File): Boolean = true

        override suspend fun extract(zipFile: File, destinationDir: File): Boolean = true
    }

    private class SuspendingZipExtractor(
        private val gate: CompletableDeferred<Unit>,
    ) : ZipExtractor {
        override suspend fun verify(zipFile: File): Boolean {
            gate.await()
            return true
        }

        override suspend fun extract(zipFile: File, destinationDir: File): Boolean = true
    }

    private class ToggleZipExtractor(
        var verifyResult: Boolean = true,
    ) : ZipExtractor {
        override suspend fun verify(zipFile: File): Boolean = verifyResult

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
        const val INSTALLED_VERSION = "0.1.1000"
        const val UPDATE_CONTENT = "update"
    }
}
