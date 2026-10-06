import data.TokenProviderImpl
import data.repository.AuthorizationImpl
import data.storage.createAuthStateStorage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Инфраструктурные Koin-модули: MAX-мост, авторизация, сеть.
 *
 * Не зависят от Compose и навигации, поэтому используются и полным
 * приложением (через DiProvider из core:di), и лёгким гейтом webShell,
 * у которого собственная точка входа без Compose.
 */
object InfraModules {
    /** Модуль MAX-моста (инициализация Mini App, авторизация MAX). */
    private val maxMiniAppModule = module {
        single<QrCodeScanner> { createQrCodeScanner() }
        single<MaxAuthorization> { createAuthorizationObject() }
    }

    /** Модуль кор-авторизации. */
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

    /** Модуль платформо-зависимых Ktor-клиентов. */
    private val networkModule = module {
        single<KtorClient> { createKtorClient(inject()) }
    }

    /** Полный набор инфраструктурных модулей. */
    val all: List<Module> = listOf(maxMiniAppModule, authorizationModule, networkModule)
}
