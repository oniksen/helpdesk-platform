package startup

import AppStartupGate
import AppUpdater
import UpdateDecision
import UpdateScreenRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CancellationException
import shouldSkipUpdate

/**
 * Проверка версии сборки при каждом запуске приложения.
 *
 * Экран обновления доступен только когда приложение запущено из shell, поэтому
 * для уже установленной сборки проверка выполняется здесь: если сервер отдаёт
 * более новую версию, приложение открывает экран обновления.
 *
 * @param updater источник данных о наличии обновления.
 * @param isUpdateSkippable определяет, обслуживает ли Service Worker текущую
 * страницу собственной сборкой. Если нет, запуск решает shell.
 */
class UpdateStartupGate(
    private val updater: AppUpdater,
    private val isUpdateSkippable: () -> Boolean = { shouldSkipUpdate() },
) : AppStartupGate {
    private var checked = false

    override suspend fun resolveStartupRoute(): NavKey? {
        if (checked) return null
        checked = true

        if (!isUpdateSkippable()) {
            println("[DIAG] startup gate: сборка не обслуживается Service Worker, проверка пропущена")
            return null
        }

        val decision = try {
            updater.checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            println("[DIAG] startup gate: ошибка проверки версии ${e::class.simpleName}: ${e.message}")
            return null
        }

        println("[DIAG] startup gate: решение проверки=${decision::class.simpleName}")
        return when (decision) {
            is UpdateDecision.UpdateAvailable -> UpdateScreenRoute
            is UpdateDecision.UpToDate, is UpdateDecision.ManifestError -> null
        }
    }
}
