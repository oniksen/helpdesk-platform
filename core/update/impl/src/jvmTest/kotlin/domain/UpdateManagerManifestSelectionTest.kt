package domain

import CurrentChannelProvider
import DownloadStatus
import domain.model.response.access.DevChannelAccess
import domain.model.response.action.ActionResponse
import domain.models.manifest.v2.UpdateManifest
import domain.port.UpdateDownloader
import domain.port.VersionActions
import domain.port.ZipExtractor
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import platform.AppInstallDirResolver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Тесты решения о наличии обновления: выбор канала с фолбэком на prod,
 * выбор платформенного таргета и сравнение версий.
 *
 * Сеть не используется: манифест отдаёт фейковый [UpdateDownloader].
 */
class UpdateManagerManifestSelectionTest {

    @Test
    fun `falls back to prod when channel provider returns nothing`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = "0.1.1001"),
            canary = ManifestFixtures.release(windows = INSTALLED_VERSION),
        )

        val loaded = createManager(manifest, channel = null).loadManifest(INSTALLED_VERSION)

        assertNotNull(loaded, "Манифест не выбран — фолбэк на prod не сработал")
        assertEquals(
            "0.1.1001",
            loaded.targetFor(UpdateChannel.PROD, UpdatePlatform.WINDOWS)?.version,
            "Выбран не prod-релиз",
        )
    }

    @Test
    fun `uses channel reported by channel provider`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = "0.1.1001"),
            canary = ManifestFixtures.release(windows = "0.1.1002"),
            rc = ManifestFixtures.release(windows = "0.1.1003"),
        )

        val cases = listOf(
            UpdateChannel.PROD to "0.1.1001",
            UpdateChannel.DEV_CANARY to "0.1.1002",
            UpdateChannel.DEV_RC to "0.1.1003",
        )

        cases.forEach { (channel, expectedVersion) ->
            val loaded = createManager(manifest, channel).loadManifest(INSTALLED_VERSION)

            assertEquals(
                expectedVersion,
                loaded?.targetFor(channel, UpdatePlatform.WINDOWS)?.version,
                "Для канала '$channel' выбран не тот релиз",
            )
        }
    }

    @Test
    fun `returns null when installed version is up to date`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = INSTALLED_VERSION),
        )

        assertNull(
            createManager(manifest, UpdateChannel.PROD).loadManifest(INSTALLED_VERSION),
            "Обновление предложено при совпадении версий",
        )
    }

    @Test
    fun `returns null when installed version is newer than available`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = "0.1.1001"),
        )

        assertNull(
            createManager(manifest, UpdateChannel.PROD).loadManifest("0.1.1002"),
            "Предложено понижение версии",
        )
    }

    @Test
    fun `does not fall back to another channel when the selected target is unpublished`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = "0.1.1001"),
            canary = ManifestFixtures.release(macos = "0.1.1002"),
        )

        assertNull(
            createManager(manifest, UpdateChannel.DEV_CANARY).loadManifest(INSTALLED_VERSION),
            "Обновление взято из чужого канала при неопубликованном таргете",
        )
    }

    @Test
    fun `returns null when prod target is unpublished while canary is published`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(macos = "0.1.1001"),
            canary = ManifestFixtures.release(windows = "0.1.1002"),
        )

        assertNull(
            createManager(manifest, channel = null).loadManifest(INSTALLED_VERSION),
            "Для prod выбрана сборка из другого канала",
        )
    }

    @Test
    fun `returns null when the manifest is unavailable`() = runBlocking {
        assertNull(
            createManager(manifest = null, channel = UpdateChannel.PROD).loadManifest(INSTALLED_VERSION),
            "Манифест выбран при ошибке загрузки",
        )
    }

    @Test
    fun `returns null for unsupported platform`() = runBlocking {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = "0.1.1001"),
        )

        assertNull(
            createManager(manifest, UpdateChannel.PROD, platform = null).loadManifest(INSTALLED_VERSION),
            "Обновление выбрано на неподдерживаемой платформе",
        )
    }

    /** Менеджер с фейковым манифестом: остальные зависимости в этих тестах не участвуют. */
    private fun createManager(
        manifest: UpdateManifest?,
        channel: String?,
        platform: UpdatePlatform? = UpdatePlatform.WINDOWS,
    ): UpdateManagerImpl = UpdateManagerImpl(
        updateDownloader = object : UpdateDownloader {
            override suspend fun downloadUpdateManifest(): UpdateManifest? = manifest

            override suspend fun downloadWithProgress(url: String, destinationFilePath: String) =
                emptyFlow<DownloadStatus>()
        },
        zipExtractor = object : ZipExtractor {
            override suspend fun verify(zipFile: File): Boolean = error("Распаковка в тестах не нужна")

            override suspend fun extract(zipFile: File, destinationDir: File): Boolean =
                error("Распаковка в тестах не нужна")
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
            override fun provide(): String? = channel
        },
        platform = platform,
    )

    private companion object {
        /** Техническая версия вида X.Y.Z: 1000 — alpha, сборка 0. */
        const val INSTALLED_VERSION = "0.1.1000"
    }
}
