import login.LoginStrategy
import login.MaxLoginStrategy
import org.koin.dsl.module

object WebAuthModules {
    val maxBridge = module {
        single<QrCodeScanner> { createQrCodeScanner() }
    }

    val login = module {
        single<MaxAuthorization> { createAuthorizationObject() }
        single<LoginStrategy> { MaxLoginStrategy(
            platformAuthorization = get(),
            maxAuthorization = get(),
        ) }
    }
}
