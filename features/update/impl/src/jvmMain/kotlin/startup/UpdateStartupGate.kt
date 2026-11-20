package startup

import AppStartupGate
import AppUpdater
import UpdateDecision
import UpdateScreenRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CancellationException

/**
 * Проверка версии сборки при каждом запуске приложения.
 *
 * Если сервер отдаёт более новую версию, приложение открывает экран обновления.
 *
 * @param updater источник данных о наличии обновления.
 * @param isUpdateSkippable определяет, нужно ли выполнять проверку при запуске.
 */
class UpdateStartupGate(
    private val updater: AppUpdater,
    private val isUpdateSkippable: () -> Boolean,
) : AppStartupGate {
    private var checked = false

    // Сбой проверки версии не должен блокировать запуск: приложение открывается
    // на текущей сборке, а обновление предложит экран обновления позже.
    @Suppress("SwallowedException")
    override suspend fun resolveStartupRoute(): NavKey? {
        if (checked) return null
        checked = true

        if (!isUpdateSkippable()) {
            return null
        }

        val decision = try {
            updater.checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return null
        }

        return when (decision) {
            is UpdateDecision.UpdateAvailable -> UpdateScreenRoute
            is UpdateDecision.UpToDate, is UpdateDecision.ManifestError -> null
        }
    }
}
