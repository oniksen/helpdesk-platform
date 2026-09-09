import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import navigation.HomePageModule
import navigation.HomePageRoute
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
        single<Authorization> {
            AuthorizationImpl(
                maxAuthorization = get(),
            )
        }
    }

    // Создаем модуль навигации, где стэк предоставляется через Koin
    private val navigationModule = module {
        // Регистрируем модули фич
        // Инициализация должна проводиться таким образом, иначе следующий фича-модуль перезапишет предыдущий.
        single { HomePageModule() } bind FeatureNavModule::class
        single { ParkingModule() } bind FeatureNavModule::class
    }

    @Composable
    fun MainKoinApplication() {
        // Вместо KoinApplication лучше использовать стандартный старт,
        // это гарантирует, что граф Koin готов ДО начала работы Compose UI.
        remember {
            startKoin {
                modules(maxMiniAppModule, navigationModule, authorizationModule)
            }
        }

        BasicDslContainer(startRoute = HomePageRoute)
    }
}