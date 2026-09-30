package startup

import AppUpdater
import AppException
import UpdateDecision
import UpdateScreenRoute
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateStartupGateTest {

    @Test
    fun `opens update screen when a newer version is available`() = runTest {
        val gate = UpdateStartupGate(
            updater = FakeAppUpdater(UpdateDecision.UpdateAvailable(UPDATE_URL)),
            isUpdateSkippable = { true },
        )

        assertEquals(UpdateScreenRoute, gate.resolveStartupRoute(), "Ожидался переход на экран обновления")
    }

    @Test
    fun `keeps the current build when no newer version exists`() = runTest {
        PASS_DECISIONS.forEach { decision ->
            val gate = UpdateStartupGate(FakeAppUpdater(decision), isUpdateSkippable = { true })

            assertNull(gate.resolveStartupRoute(), "Неожиданный переход для решения $decision")
        }
    }

    @Test
    fun `does not check the version when the build is not served by service worker`() = runTest {
        val updater = FakeAppUpdater(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val gate = UpdateStartupGate(updater, isUpdateSkippable = { false })

        assertNull(gate.resolveStartupRoute(), "Проверка не должна прерывать запуск shell")
        assertEquals(0, updater.checkCount, "Обращение к сети не ожидалось: запуск решает shell")
    }

    @Test
    fun `keeps the current build when the version check fails`() = runTest {
        val gate = UpdateStartupGate(
            updater = FailingAppUpdater(),
            isUpdateSkippable = { true },
        )

        assertNull(gate.resolveStartupRoute(), "Ошибка проверки не должна блокировать запуск")
    }

    @Test
    fun `checks the version only once per launch`() = runTest {
        val updater = FakeAppUpdater(UpdateDecision.UpdateAvailable(UPDATE_URL))
        val gate = UpdateStartupGate(updater, isUpdateSkippable = { true })

        gate.resolveStartupRoute()
        val second = gate.resolveStartupRoute()

        assertNull(second, "Повторная проверка в рамках одного запуска не нужна")
        assertEquals(1, updater.checkCount, "Ожидалась ровно одна проверка версии")
    }

    private class FakeAppUpdater(
        private val decision: UpdateDecision,
    ) : AppUpdater {
        var checkCount: Int = 0
            private set

        override suspend fun checkForUpdate(): UpdateDecision {
            checkCount++
            return decision
        }

        override suspend fun downloadAndUnpack(url: String, forceRefresh: Boolean): Map<String, ByteArray> =
            emptyMap()

        override suspend fun getCachedBuild(url: String): Map<String, ByteArray>? = null

        override suspend fun clearCache() = Unit
    }

    private class FailingAppUpdater : AppUpdater {
        override suspend fun checkForUpdate(): UpdateDecision =
            throw IllegalStateException("Манифест недоступен")

        override suspend fun downloadAndUnpack(url: String, forceRefresh: Boolean): Map<String, ByteArray> =
            emptyMap()

        override suspend fun getCachedBuild(url: String): Map<String, ByteArray>? = null

        override suspend fun clearCache() = Unit
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
