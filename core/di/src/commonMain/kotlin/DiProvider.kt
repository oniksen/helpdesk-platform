import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import data.TokenProviderImpl
import data.repository.AuthorizationImpl
import data.storage.createAuthStateStorage
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

/**
 * Точка входа в граф зависимостей.
 *
 * Регистрирует только инфраструктурные модули (MAX-мост, авторизация,
 * сеть, обновления), а состав приложения (фичи и стартовый экран)
 * задаётся через [AppDefinition].
 */
class DiProvider(
    private val definition: AppDefinition,
) {
    private val maxMiniAppModule = module {
        single<QrCodeScanner> { createQrCodeScanner() }
        single<MaxAuthorization> { createAuthorizationObject() }
    }

    // Модуль кор-авторизации используя MAX API.
    private val authorizationModule = module {
        single<Authorization> {
            AuthorizationImpl(
                maxAuthorization = get(),
                client = get(),
                storage = createAuthStateStorage(),
            )
        }
        single<TokenProvider> {
            TokenProviderImpl(
                authorization = get()
            )
        }
    }

    // Модуль для предоставления платформо-зависимых реализаций сетевых Ktor клиентов.
    private val networkModule = module {
        single<KtorClient> { createKtorClient(inject()) }
    }

    // Модуль обновлений (загрузка сборок с сервера)
    private val updateModule = module {
        single<AppUpdater> { createAppUpdater() }
    }

    @Composable
    fun MainKoinApplication() {
        val koinApplication = remember {
            startKoin {
                modules(
                    maxMiniAppModule,
                    authorizationModule,
                    networkModule,
                    updateModule,
                    *definition.koinModules.toTypedArray(),
                )
            }
        }

        val startRoute = remember {
            definition.startRoute(koinApplication.koin)
        }

        RootNavigation(startRoute = startRoute)
    }
}
