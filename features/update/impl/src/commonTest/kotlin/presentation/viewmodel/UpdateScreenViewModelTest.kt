package presentation.viewmodel

import AppNavigator
import AppUpdater
import AppException
import DEFAULT_WEB_APP_URL
import UpdateDecision
import dev.mokkery.MockMode.autoUnit
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verify.VerifyMode.Companion.not
import dev.mokkery.verifySuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import presentation.effect.UpdateScreenEffect
import presentation.intent.UpdateScreenIntent
import presentation.state.UpdateState
import presentation.state.UpdateStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateScreenViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.resetMain()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `skips installation when the current build is served by service worker`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpToDate)
        val installs = CountingFinishInstall()
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { true },
            finishInstall = installs.install,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        verifySuspend(not) { updater.getCachedBuild(any()) }
        verifySuspend(not) { updater.downloadAndUnpack(any(), any()) }
        assertEquals(1, installs.count, "Установка должна завершиться ровно один раз")
        assertEquals(UpdateState(), viewModel.state.value, "Пропуск обновления не должен менять состояние экрана")
    }

    @Test
    fun `installs the cached build when no new version exists`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpToDate, CACHED_BUILD)
        val push = RecordingBuildPush()
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            pushBuild = push.push,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        verifySuspend(not) { updater.downloadAndUnpack(any(), any()) }
        assertEquals(CACHED_BUILD, push.build, "В Service Worker должна уйти сборка из кэша")
        assertSuccess(viewModel)
    }

    @Test
    fun `ignores the cached build when a new version exists`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL), CACHED_BUILD)
        val push = RecordingBuildPush()
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            pushBuild = push.push,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        verifySuspend(not) { updater.getCachedBuild(any()) }
        verifySuspend { updater.downloadAndUnpack(DEFAULT_WEB_APP_URL, true) }
        assertEquals(DOWNLOADED_BUILD, push.build, "В Service Worker должна уйти свежескачанная сборка")
        assertSuccess(viewModel)
    }

    @Test
    fun `installs the cached build when the version check fails`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(MANIFEST_ERROR, CACHED_BUILD)
        val push = RecordingBuildPush()
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            pushBuild = push.push,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        verifySuspend(not) { updater.downloadAndUnpack(any(), any()) }
        assertEquals(CACHED_BUILD, push.build, "Ошибка проверки версии не должна мешать установке кэша")
        assertSuccess(viewModel)
    }

    @Test
    fun `reports an error when service worker does not take control`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val push = RecordingBuildPush()
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            registerServiceWorker = { false },
            pushBuild = push.push,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertNull(push.build, "Без контроля Service Worker сборка не должна передаваться")
        assertError(viewModel, "Service Worker недоступен: установка не может завершиться")
    }

    @Test
    fun `reports an error when the build cannot be handed to service worker`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            pushBuild = { _, _ -> false },
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertError(viewModel, "Не удалось передать сборку в Service Worker")
    }

    @Test
    fun `reports an error when the build download fails`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = mock<AppUpdater>(autoUnit) {
            everySuspend { checkForUpdate() } returns UpdateDecision.UpdateAvailable(UPDATE_URL)
            everySuspend { getCachedBuild(DEFAULT_WEB_APP_URL) } returns null
            everySuspend { downloadAndUnpack(any(), any()) } throws DOWNLOAD_ERROR
        }
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertError(viewModel, DOWNLOAD_ERROR.message!!)
    }

    @Test
    fun `reports an error when the install does not switch to the new build`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            finishInstall = { FinishInstallResult.Failed },
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertError(
            viewModel,
            "Установка завершена, но приложение не переключилось на новую сборку. Обновите страницу вручную.",
        )
    }

    @Test
    fun `starts over when the screen asks for another attempt`() = runTest {
        val dispatcher = mainDispatcher()
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val viewModel = createViewModel(
            updater = updater,
            isUpdateSkippable = { false },
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        viewModel.sendIntent(UpdateScreenIntent.StartUpdate)
        advanceUntilIdle()

        verifySuspend(exactly(2)) { updater.checkForUpdate() }
        verifySuspend(exactly(2)) { updater.downloadAndUnpack(DEFAULT_WEB_APP_URL, true) }
        assertSuccess(viewModel)
    }

    /**
     * Диспетчеры экрана и фоновой работы привязаны к планировщику теста: иначе
     * `advanceUntilIdle` не дождётся виртуальных задержек распаковки и установки.
     * */
    private fun TestScope.mainDispatcher(): CoroutineDispatcher =
        StandardTestDispatcher(testScheduler).also { Dispatchers.setMain(it) }

    private fun createViewModel(
        updater: AppUpdater,
        isUpdateSkippable: () -> Boolean,
        dispatcher: CoroutineDispatcher,
        registerServiceWorker: suspend () -> Boolean = { true },
        pushBuild: suspend (url: String, files: Map<String, ByteArray>) -> Boolean = { _, _ -> true },
        finishInstall: suspend () -> FinishInstallResult = { FinishInstallResult.Reloaded },
    ) = UpdateScreenViewModel(
        navigator = mock<AppNavigator>(autoUnit),
        updater = updater,
        isUpdateSkippable = isUpdateSkippable,
        registerServiceWorker = registerServiceWorker,
        pushBuild = pushBuild,
        finishInstall = finishInstall,
        backgroundDispatcher = dispatcher,
    )

    private fun updaterReturning(
        decision: UpdateDecision,
        cachedBuild: Map<String, ByteArray>? = null,
    ) = mock<AppUpdater>(autoUnit) {
        everySuspend { checkForUpdate() } returns decision
        everySuspend { getCachedBuild(DEFAULT_WEB_APP_URL) } returns cachedBuild
        everySuspend { downloadAndUnpack(DEFAULT_WEB_APP_URL, any()) } returns DOWNLOADED_BUILD
    }

    private fun assertSuccess(viewModel: UpdateScreenViewModel) {
        val state = viewModel.state.value
        assertEquals(UpdateStatus.Success, state.status, "Ожидался успешный статус установки: $state")
        assertEquals(SUCCESS_MESSAGE, state.message, "Неверное сообщение об успешной установке: $state")
        assertFalse(state.inProgress, "После установки индикатор должен быть выключен: $state")
    }

    private fun assertError(viewModel: UpdateScreenViewModel, expectedSnackBar: String) {
        val state = viewModel.state.value
        assertEquals(UpdateStatus.Error, state.status, "Ожидался ошибочный статус: $state")
        assertEquals(ERROR_MESSAGE, state.message, "Неверное сообщение об ошибке: $state")
        assertFalse(state.inProgress, "После ошибки индикатор должен быть выключен: $state")
        assertEquals(
            listOf(UpdateScreenEffect.ShowSnackBar(expectedSnackBar)),
            viewModel.effect.replayCache,
            "Ожидался snackbar с текстом ошибки",
        )
    }

    /**
     * Инстансы классов, реализующих suspend-function-type, на Kotlin/JS не вызываются
     * как функции, поэтому обработчики передаются лямбдами.
     * */
    private class CountingFinishInstall {
        var count: Int = 0
            private set

        val install: suspend () -> FinishInstallResult = {
            count++
            FinishInstallResult.Reloaded
        }
    }

    private class RecordingBuildPush {
        var build: Map<String, ByteArray>? = null
            private set

        val push: suspend (String, Map<String, ByteArray>) -> Boolean = { _, files ->
            build = files
            true
        }
    }

    private companion object {
        const val UPDATE_URL = "https://helpdesk.lpmti.ru/helpdesk-app/build.zip"
        const val SUCCESS_MESSAGE = "Успешная установка"
        const val ERROR_MESSAGE = "Ошибка обновления. Повторите снова"

        val MANIFEST_ERROR = UpdateDecision.ManifestError(
            AppException.Network.NoInternet("Нет подключения к интернету"),
        )
        val DOWNLOAD_ERROR = IllegalStateException("Ошибка загрузки обновления: HTTP 503")
        val CACHED_BUILD = mapOf("index.html" to "cached".encodeToByteArray())
        val DOWNLOADED_BUILD = mapOf("index.html" to "downloaded".encodeToByteArray())
    }
}
