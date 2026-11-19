package domain

import AppException
import AppVersion
import AppVersionProvider
import DownloadStatus
import data.impl.ChannelAccessStoreImpl
import domain.model.ChannelAccessSource
import domain.model.CheckUpdateState
import domain.model.UnavailableReason
import domain.model.UpdateChannel as TypedChannel
import domain.models.channels.ChannelAccess
import domain.models.channels.ChannelsFile
import domain.models.manifest.v2.UpdateManifest
import domain.port.ChannelAccessStore
import domain.port.UpdateDownloader
import domain.port.ZipExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import normalizeFromTechnical
import platform.AppInstallDirResolver
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Проверка обновления: fallback каналов, таргет платформы, сравнение версий
 * и публикация терминального состояния.
 */
class AppUpdateManagerCheckTest {

    @Test
    fun `falls back to prod when channels manifest is unavailable`() = runBlocking {
        val manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002")
        val downloader = FakeUpdateDownloader(
            manifest = manifest,
            channelsError = IOException("channels down"),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.PROD, state.update.channel, "При ошибке каналов должен использоваться prod")
        assertEquals(
            ChannelAccessSource.FALLBACK,
            manager.channelAccessState.value.source,
            "Источник каналов должен быть помечен как fallback",
        )
        assertEquals(
            TypedChannel.PROD,
            manager.channelAccessState.value.current,
            "Эффективный канал должен быть prod",
        )
    }

    @Test
    fun `does not request channels when email is missing`() = runBlocking {
        val downloader = FakeUpdateDownloader(
            manifest = manifestFor(prod = "0.1.1001"),
            channels = channels(),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = null)

        assertEquals(
            0,
            downloader.channelsCalls,
            "При отсутствии email channels.json не должен запрашиваться",
        )
        assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.PROD, manager.channelAccessState.value.current, "Должен использоваться prod")
        assertEquals(
            ChannelAccessSource.FALLBACK,
            manager.channelAccessState.value.source,
            "Источник должен быть fallback",
        )
    }

    @Test
    fun `does not request channels for blank email`() = runBlocking {
        val downloader = FakeUpdateDownloader(
            manifest = manifestFor(prod = "0.1.1001"),
            channels = channels(),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "   ")

        assertEquals(
            0,
            downloader.channelsCalls,
            "Для пустого email channels.json не должен запрашиваться",
        )
        assertEquals(
            TypedChannel.PROD,
            manager.channelAccessState.value.current,
            "Для пустого email должен использоваться prod",
        )
    }

    @Test
    fun `does not reuse stored channels when email is missing`() = runBlocking {
        val store = ChannelAccessStoreImpl().apply {
            save(ChannelAccess(allowed = listOf("dev.canary"), selected = "dev.canary"))
        }
        val manager = createManager(
            downloader = FakeUpdateDownloader(
                manifest = manifestFor(prod = "0.1.1001"),
                channels = channels(),
            ),
            store = store,
        )

        manager.checkUpdate(email = null)

        val snapshot = manager.channelAccessState.value
        assertEquals(emptyList(), snapshot.allowed, "Без email нельзя показывать сохранённые каналы")
        assertNull(snapshot.selected, "Без email нельзя показывать сохранённый выбранный канал")
        assertEquals(TypedChannel.PROD, snapshot.current, "Без email эффективный канал — prod")
        assertEquals(ChannelAccessSource.FALLBACK, snapshot.source, "Источник должен быть fallback")
    }

    @Test
    fun `uses selected channel when it is allowed`() = runBlocking {
        val manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002")
        val access = ChannelAccess(allowed = listOf("dev.canary"), selected = "dev.canary")
        val downloader = FakeUpdateDownloader(
            manifest = manifest,
            channels = channels(users = mapOf("user@corp.ru" to access)),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.DEV_CANARY, state.update.channel, "Должен использоваться выбранный canary")
        assertEquals(
            "0.1.1002".normalizeFromTechnical(),
            state.update.version,
            "Версия должна быть из canary",
        )
    }

    @Test
    fun `uses first allowed channel when selected is not allowed`() = runBlocking {
        val manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002", rc = "0.1.1003")
        val access = ChannelAccess(allowed = listOf("dev.rc"), selected = "dev.canary")
        val downloader = FakeUpdateDownloader(
            manifest = manifest,
            channels = channels(users = mapOf("user@corp.ru" to access)),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(
            TypedChannel.DEV_RC,
            state.update.channel,
            "При недопустимом selected должен использоваться первый allowed",
        )
        assertEquals(
            TypedChannel.DEV_CANARY,
            manager.channelAccessState.value.selected,
            "Сырой выбранный канал должен остаться в снимке",
        )
        assertEquals(
            TypedChannel.DEV_RC,
            manager.channelAccessState.value.current,
            "Эффективный канал должен быть первым allowed",
        )
    }

    @Test
    fun `uses first allowed channel when selected is missing`() = runBlocking {
        val manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1003", rc = "0.1.1002")
        val access = ChannelAccess(allowed = listOf("dev.rc", "dev.canary"), selected = null)
        val downloader = FakeUpdateDownloader(
            manifest = manifest,
            channels = channels(users = mapOf("user@corp.ru" to access)),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.DEV_RC, state.update.channel, "Должен использоваться первый доступный канал")
        assertEquals(
            "0.1.1002".normalizeFromTechnical(),
            state.update.version,
            "Версия должна быть из rc",
        )
    }

    @Test
    fun `uses prod when neither selected nor allowed are known`() = runBlocking {
        val manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002")
        val access = ChannelAccess(allowed = listOf("dev.unknown"), selected = "dev.unknown")
        val downloader = FakeUpdateDownloader(
            manifest = manifest,
            channels = channels(users = mapOf("user@corp.ru" to access)),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.PROD, state.update.channel, "Неизвестные каналы должны приводить к prod")
    }

    @Test
    fun `ignores unknown aliases in allowed list`() = runBlocking {
        val manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002")
        val access = ChannelAccess(allowed = listOf("dev.unknown", "dev.canary"), selected = null)
        val downloader = FakeUpdateDownloader(
            manifest = manifest,
            channels = channels(users = mapOf("user@corp.ru" to access)),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.DEV_CANARY, state.update.channel, "Известный канал должен быть выбран")
        assertEquals(
            listOf(TypedChannel.DEV_CANARY),
            manager.channelAccessState.value.allowed,
            "Неизвестный алиас не должен попадать в снимок",
        )
    }

    @Test
    fun `reports unavailable when target is not published`() = runBlocking {
        val manifest = manifestFor(prod = null, canary = "0.1.1002")
        val downloader = FakeUpdateDownloader(manifest = manifest, channels = channels())
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Unavailable>(manager.checkUpdateState.value)
        assertEquals(
            UnavailableReason.NO_PUBLISHED_BUILD,
            state.reason,
            "Для неопубликованного prod должна быть причина NO_PUBLISHED_BUILD",
        )
        assertEquals(
            TypedChannel.PROD,
            manager.channelAccessState.value.current,
            "Канал не должен подменяться на canary",
        )
    }

    @Test
    fun `reports unavailable on unsupported platform`() = runBlocking {
        val downloader = FakeUpdateDownloader(manifestError = IOException("must not be called"))
        val manager = createManager(downloader, platform = null)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Unavailable>(manager.checkUpdateState.value)
        assertEquals(
            UnavailableReason.UNSUPPORTED_PLATFORM,
            state.reason,
            "На неподдерживаемой платформе должна быть причина UNSUPPORTED_PLATFORM",
        )
        assertEquals(0, downloader.manifestCalls, "На неподдерживаемой платформе манифест не запрашивается")
    }

    @Test
    fun `reports up to date when installed version is current or newer`() = runBlocking {
        listOf("0.1.1000", "0.0.1000").forEach { available ->
            val manifest = manifestFor(prod = available)
            val manager = createManager(FakeUpdateDownloader(manifest = manifest, channels = channels()))

            manager.checkUpdate(email = "user@corp.ru")

            assertEquals(
                CheckUpdateState.IsUpToDate,
                manager.checkUpdateState.value,
                "Для доступной версии '$available' ожидалось IsUpToDate",
            )
        }
    }

    @Test
    fun `reports error when manifest cannot be loaded`() = runBlocking {
        val cause = IOException("network")
        val downloader = FakeUpdateDownloader(
            manifestError = cause,
            channels = channels(),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Error>(manager.checkUpdateState.value)
        assertIs<AppException.UpdateError.CheckFailed>(
            state.error,
            "Ошибка манифеста должна быть CheckFailed",
        )
        assertSame(
            cause,
            state.error.cause,
            "Причина ошибки манифеста должна сохраняться",
        )
        assertEquals(
            TypedChannel.PROD,
            manager.channelAccessState.value.current,
            "Снимок каналов должен остаться доступным",
        )
    }

    @Test
    fun `reports error when target version is invalid`() = runBlocking {
        val manifest = manifestFor(prod = "not-a-version")
        val manager = createManager(FakeUpdateDownloader(manifest = manifest, channels = channels()))

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Error>(manager.checkUpdateState.value)
        assertEquals(
            "Не удалось разобрать версию обновления",
            state.error.message,
            "Ошибка невалидной версии должна содержать причину",
        )
        assertIs<IllegalStateException>(
            state.error.cause,
            "Причина невалидной версии должна сохраняться",
        )
        Unit
    }

    @Test
    fun `reports error when installed version cannot be read`() = runBlocking {
        val cause = IllegalStateException("version")
        val manager = createManager(
            downloader = FakeUpdateDownloader(
                manifest = manifestFor(prod = "0.1.1001"),
                channels = channels(),
            ),
            versionProvider = object : AppVersionProvider {
                override fun provide(): AppVersion = throw cause
            },
        )

        manager.checkUpdate(email = "user@corp.ru")

        val state = assertIs<CheckUpdateState.Error>(manager.checkUpdateState.value)
        assertEquals(
            "Не удалось определить текущую версию",
            state.error.message,
            "Ошибка версии приложения должна содержать причину",
        )
        assertSame(
            cause,
            state.error.cause,
            "Причина ошибки версии приложения должна сохраняться",
        )
    }

    @Test
    fun `publishes terminal state before checkUpdate returns`() = runBlocking {
        val manager = createManager(
            FakeUpdateDownloader(
                manifest = manifestFor(prod = "0.1.1001"),
                channels = channels(),
            ),
        )

        manager.checkUpdate(email = "user@corp.ru")

        assertIs<CheckUpdateState.Available>(
            manager.checkUpdateState.value,
            "После checkUpdate состояние должно быть терминальным",
        )
        Unit
    }

    @Test
    fun `does not reset terminal state to idle`() = runBlocking {
        val manager = createManager(
            FakeUpdateDownloader(
                manifest = manifestFor(prod = "0.1.1001"),
                channels = channels(),
            ),
        )
        manager.checkUpdate(email = "user@corp.ru")
        val terminal = assertIs<CheckUpdateState.Available>(
            manager.checkUpdateState.value,
            "После проверки должно быть терминальное состояние",
        )

        yield()

        assertEquals(
            terminal,
            manager.checkUpdateState.value,
            "Терминальное состояние не должно сбрасываться в Idle",
        )
    }

    @Test
    fun `keeps channel access snapshot with allowed selected and current`() = runBlocking {
        val access = ChannelAccess(
            allowed = listOf("dev.canary", "prod"),
            selected = "dev.canary",
        )
        val manager = createManager(
            FakeUpdateDownloader(
                manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002"),
                channels = channels(users = mapOf("user@corp.ru" to access)),
            ),
        )

        manager.checkUpdate(email = "user@corp.ru")

        val snapshot = manager.channelAccessState.value
        assertEquals(
            listOf(TypedChannel.DEV_CANARY, TypedChannel.PROD),
            snapshot.allowed,
            "Список доступных каналов должен сохранить порядок файла",
        )
        assertEquals(TypedChannel.DEV_CANARY, snapshot.selected, "Выбранный канал должен сохраниться")
        assertEquals(TypedChannel.DEV_CANARY, snapshot.current, "Эффективный канал должен быть canary")
        assertEquals(ChannelAccessSource.SERVER, snapshot.source, "Источник должен быть серверным")
    }

    @Test
    fun `keeps last known channels on fallback but sets current to prod`() = runBlocking {
        val access = ChannelAccess(allowed = listOf("dev.canary"), selected = "dev.canary")
        val downloader = FakeUpdateDownloader(
            manifest = manifestFor(prod = "0.1.1001", canary = "0.1.1002"),
            channels = channels(users = mapOf("user@corp.ru" to access)),
        )
        val manager = createManager(downloader)

        manager.checkUpdate(email = "user@corp.ru")
        assertEquals(
            TypedChannel.DEV_CANARY,
            manager.channelAccessState.value.current,
            "Первый успешный запрос должен выбрать canary",
        )

        downloader.channelsError = IOException("channels down")
        manager.checkUpdate(email = "user@corp.ru")

        val snapshot = manager.channelAccessState.value
        assertEquals(
            listOf(TypedChannel.DEV_CANARY),
            snapshot.allowed,
            "Последние известные каналы должны сохраниться",
        )
        assertEquals(TypedChannel.DEV_CANARY, snapshot.selected, "Выбранный канал должен сохраниться")
        assertEquals(
            TypedChannel.PROD,
            snapshot.current,
            "При fallback эффективный канал должен быть prod",
        )
        assertEquals(ChannelAccessSource.FALLBACK, snapshot.source, "Источник должен быть fallback")

        val state = assertIs<CheckUpdateState.Available>(manager.checkUpdateState.value)
        assertEquals(TypedChannel.PROD, state.update.channel, "Проверка должна идти по prod")
        assertTrue(
            state.update.version > "0.1.1000".normalizeFromTechnical(),
            "Версия обновления должна быть новее установленной",
        )
    }

    private fun createManager(
        downloader: UpdateDownloader,
        installedVersion: String = "0.1.1000",
        platform: UpdatePlatform? = UpdatePlatform.WINDOWS,
        store: ChannelAccessStore = ChannelAccessStoreImpl(),
        versionProvider: AppVersionProvider =
            FakeVersionProvider(installedVersion.normalizeFromTechnical()),
    ): AppUpdateManagerImpl = AppUpdateManagerImpl(
        updateDownloader = downloader,
        zipExtractor = NoOpZipExtractor,
        appRestarter = AppRestarter { },
        appInstallDirResolver = object : AppInstallDirResolver {
            override fun getAppInstallDir(referenceClass: Class<*>): File = error("Не используется")

            override fun getAppBundlePath(referenceClass: Class<*>): File? = null
        },
        channelAccessStore = store,
        versionProvider = versionProvider,
        platform = platform,
    )

    private fun manifestFor(
        prod: String? = null,
        canary: String? = null,
        rc: String? = null,
    ): UpdateManifest = ManifestFixtures.manifest(
        prod = ManifestFixtures.release(windows = prod),
        canary = ManifestFixtures.release(windows = canary),
        rc = ManifestFixtures.release(windows = rc),
    )

    private fun channels(
        default: ChannelAccess = ChannelAccess(allowed = emptyList(), selected = null),
        users: Map<String, ChannelAccess> = emptyMap(),
    ): ChannelsFile = ChannelsFile(default = default, users = users)

    private class FakeVersionProvider(
        private val version: AppVersion,
    ) : AppVersionProvider {
        override fun provide(): AppVersion = version
    }

    private object NoOpZipExtractor : ZipExtractor {
        override suspend fun verify(zipFile: File): Boolean = true

        override suspend fun extract(zipFile: File, destinationDir: File): Boolean = true
    }

    private class FakeUpdateDownloader(
        var manifest: UpdateManifest? = null,
        var channels: ChannelsFile? = null,
        var manifestError: Throwable? = null,
        var channelsError: Throwable? = null,
        var downloadFlow: Flow<DownloadStatus> = emptyFlow(),
    ) : UpdateDownloader {
        var manifestCalls: Int = 0
            private set
        var channelsCalls: Int = 0
            private set

        override suspend fun downloadUpdateManifest(): UpdateManifest {
            manifestCalls += 1
            manifestError?.let { throw it }
            return requireNotNull(manifest) { "Манифест не задан" }
        }

        override suspend fun downloadChannelsManifest(): ChannelsFile {
            channelsCalls += 1
            channelsError?.let { throw it }
            return requireNotNull(channels) { "Каналы не заданы" }
        }

        override suspend fun downloadWithProgress(
            url: String,
            destinationFilePath: String,
        ): Flow<DownloadStatus> = downloadFlow
    }
}
