package domain

import CurrentChannelProvider
import DownloadStatus
import domain.model.response.access.DevChannelAccess
import domain.model.response.action.ActionResponse
import domain.port.UpdateDownloader
import domain.port.VersionActions
import domain.port.ZipExtractor
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import platform.AppInstallDirResolver
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тесты установки файлов обновления: резервная копия заменяемого файла,
 * сохранность встроенного рантайма и очистка резервных копий.
 */
class UpdateManagerInstallFilesTest {

    private lateinit var manager: UpdateManagerImpl
    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        manager = createManager()
        workDir = Files.createTempDirectory("update-install-test").toFile()
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

        manager.replaceFilesSafely(sourceDir, installDir)

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

            manager.replaceFilesSafely(sourceDir, installDir)

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

        manager.cleanupBackupFiles(installDir)

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

    private fun createDir(name: String): File = File(workDir, name).apply { mkdirs() }

    private fun createFile(directory: File, path: String, content: String): File =
        File(directory, path).apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    /** Менеджер с заглушками: тестам нужны только работа с файлами. */
    private fun createManager(): UpdateManagerImpl = UpdateManagerImpl(
        updateDownloader = object : UpdateDownloader {
            override suspend fun downloadUpdateManifest(): Nothing? = null
            override suspend fun downloadWithProgress(url: String, destinationFilePath: String) =
                emptyFlow<DownloadStatus>()
        },
        zipExtractor = object : ZipExtractor {
            override suspend fun verify(zipFile: File): Boolean = true
            override suspend fun extract(zipFile: File, destinationDir: File): Boolean = true
        },
        appRestarter = AppRestarter { error("Перезапуск в тестах не нужен") },
        appInstallDirResolver = object : AppInstallDirResolver {
            override fun getAppInstallDir(referenceClass: Class<*>): File = error("Не используется")
            override fun getAppBundlePath(referenceClass: Class<*>): File? = null
        },
        versionActions = object : VersionActions {
            override suspend fun markAsStable(): ActionResponse? = null
            override suspend fun reject(reason: String): ActionResponse? = null
            override suspend fun publish(): ActionResponse? = null
            override suspend fun devChannelAccess(): DevChannelAccess? = null
        },
        channelProvider = object : CurrentChannelProvider {
            override fun provide(): String? = null
        },
        platform = UpdatePlatform.WINDOWS,
    )

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
