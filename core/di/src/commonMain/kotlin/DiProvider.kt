import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin as startKoinContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Точка входа в граф зависимостей полного приложения.
 *
 * Регистрирует инфраструктурные модули ([InfraModules]) и провайдер
 * версии приложения, а состав приложения (фичи и стартовый экран)
 * задаётся через [AppDefinition].
 *
 * Лёгкий гейт webShell [DiProvider] не использует — он поднимает Koin
 * сам через [InfraModules] без Compose.
 */
class DiProvider(
    private val definition: AppDefinition,
) {
    // Модуль версии приложения (отображение в настройках).
    private val updateModule = module {
        single<AppVersionProvider> { AppVersionProviderImpl() }
    }

    /**
     * Поднимает граф зависимостей приложения вне Compose —
     * это позволяет проверить сессию до первого кадра интерфейса.
     */
    fun startKoin(): KoinApplication = startKoinContext {
        modules(
            *InfraModules.all.toTypedArray(),
            updateModule,
            *definition.koinModules.toTypedArray(),
        )
    }

    /** Compose-корень приложения: стартовый маршрут и рутовая навигация. */
    @Composable
    fun MainKoinApplication(koinApplication: KoinApplication) {
        val startRoute = remember {
            definition.startRoute(koinApplication.koin)
        }

        RootNavigation(startRoute = startRoute)
    }
}
