import androidx.navigation3.runtime.NavKey
import org.koin.core.Koin
import org.koin.core.module.Module

/**
 * Определение конкретного приложения: какие модули фич регистрируются
 * и с какого экрана стартует приложение.
 *
 * Позволяет собирать разные точка входа (webApp, webShell, будущие JVM)
 * из общего ядра без внесения зависимостей от фич в [core:di].
 */
interface AppDefinition {
    /** Koin-модули рутовой и фич-навигации приложения. */
    val koinModules: List<Module>

    /** Стартовый маршрут приложения. */
    fun startRoute(koin: Koin): NavKey
}
