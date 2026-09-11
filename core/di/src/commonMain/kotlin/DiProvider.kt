import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import data.network.KtorClient
import data.repository.AuthorizationImpl
import navigation.AuthorizationScreenModule
import navigation.HomePageModule
import navigation.ParkingModule
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module

class DiProvider {
    private val maxMiniAppModule = module {
        single<QrCodeScanner> { createQrCodeScanner() }
        single<MaxAuthorization> { createAuthorizationObject() }
    }

    private val authorizationModule = module {
        single<KtorClient> { KtorClient() }
        single<Authorization> {
            AuthorizationImpl(
                maxAuthorization = get(),
                client = get(),
            )
        }
    }

    // Модуль фич-навигации (внутренняя навигация MainNavigation)
    private val featuresNavModule = module {
        single { ParkingModule() } bind FeatureNavModule::class
        single { HomePageModule() } bind FeatureNavModule::class
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
                    rootNavModule,
                )
            }
        }

        RootNavigation(startRoute = AuthorizationScreenRoute)
    }
}