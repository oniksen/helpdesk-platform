import login.CredentialsLoginStrategy
import login.LoginStrategy
import org.koin.dsl.module

object JvmAuthModules {

    val login = module {
        single<LoginStrategy> { CredentialsLoginStrategy(platformAuthorization = get()) }
    }
}
