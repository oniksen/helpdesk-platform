import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import data.TokenProviderImpl
import data.repository.AuthorizationImpl
import navigation.AuthorizationScreenModule
import navigation.TasksPageModule
import navigation.ParkingModule
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module

class DiProvider {
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

    // Модуль фич-навигации (внутренняя навигация MainNavigation)
    private val featuresNavModule = module {
        single { ParkingModule() } bind FeatureNavModule::class
        single { TasksPageModule() } bind FeatureNavModule::class
    }

    // Модуль рутовой навигации (Auth ↔ Home)
    private val rootNavModule = module {
        single { AuthorizationScreenModule() } bind RootNavModule::class
        single { HomePageContainerModule() } bind RootNavModule::class
    }

    @Composable
    fun MainKoinApplication() {
        remember {
            startKoin {
                modules(
                    maxMiniAppModule,
                    featuresNavModule,
                    authorizationModule,
                    networkModule,
                    rootNavModule,
                )
            }
        }

        RootNavigation(startRoute = AuthorizationScreenRoute)
    }
}