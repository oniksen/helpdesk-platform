package startup

import AppUpdater
import AppException
import UpdateDecision
import UpdateScreenRoute
import dev.mokkery.MockMode.autoUnit
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verify.VerifyMode.Companion.not
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateStartupGateTest {

    @Test
    fun `opens update screen when a newer version is available`() = runTest {
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val gate = UpdateStartupGate(updater = updater, isUpdateSkippable = { true })

        assertEquals(UpdateScreenRoute, gate.resolveStartupRoute(), "Ожидался переход на экран обновления")
    }

    @Test
    fun `keeps the current build when no newer version exists`() = runTest {
        PASS_DECISIONS.forEach { decision ->
            val gate = UpdateStartupGate(updaterReturning(decision), isUpdateSkippable = { true })

            assertNull(gate.resolveStartupRoute(), "Неожиданный переход для решения $decision")
        }
    }

    @Test
    fun `does not check the version when the build is not served by service worker`() = runTest {
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val gate = UpdateStartupGate(updater = updater, isUpdateSkippable = { false })

        assertNull(gate.resolveStartupRoute(), "Проверка не должна прерывать запуск shell")
        verifySuspend(not) { updater.checkForUpdate() }
    }

    @Test
    fun `keeps the current build when the version check fails`() = runTest {
        val updater = mock<AppUpdater>(autoUnit) {
            everySuspend { checkForUpdate() } throws IllegalStateException("Манифест недоступен")
        }
        val gate = UpdateStartupGate(updater = updater, isUpdateSkippable = { true })

        assertNull(gate.resolveStartupRoute(), "Ошибка проверки версии не должна блокировать запуск приложения")
    }

    @Test
    fun `checks the version only once per launch`() = runTest {
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val gate = UpdateStartupGate(updater = updater, isUpdateSkippable = { true })

        gate.resolveStartupRoute()
        val second = gate.resolveStartupRoute()

        assertNull(second, "Повторный запуск не должен открывать экран обновления ещё раз")
        verifySuspend(exactly(1)) { updater.checkForUpdate() }
    }

    @Test
    fun `resolves the route without touching the build cache`() = runTest {
        val updater = updaterReturning(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val gate = UpdateStartupGate(updater = updater, isUpdateSkippable = { true })

        gate.resolveStartupRoute()

        verifySuspend(not) { updater.getCachedBuild(any()) }
        verifySuspend(not) { updater.downloadAndUnpack(any(), any()) }
    }

    private fun updaterReturning(decision: UpdateDecision) = mock<AppUpdater>(autoUnit) {
        everySuspend { checkForUpdate() } returns decision
    }

    private companion object {
        const val UPDATE_URL = "https://helpdesk.lpmti.ru/helpdesk-app/build.zip"

        val PASS_DECISIONS = listOf(
            UpdateDecision.UpToDate,
            UpdateDecision.ManifestError(
                AppException.Network.NoInternet("Нет подключения к интернету")
            ),
        )
    }
}
