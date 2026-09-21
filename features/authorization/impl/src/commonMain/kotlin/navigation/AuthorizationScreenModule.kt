package navigation

import AppNavigator
import AuthorizationScreenRoute
import RootNavModule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import presentation.screen.AuthorizationScreen
import presentation.viewmodel.AuthorizationScreenViewModel

class AuthorizationScreenModule : RootNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(baseClass = NavKey::class) {
            subclass(AuthorizationScreenRoute::class, AuthorizationScreenRoute.serializer())
        }
    }

    override fun canResolve(key: NavKey): Boolean = key is AuthorizationScreenRoute

    override fun resolve(
        key: NavKey,
        navigator: AppNavigator
    ): NavEntry<*> = NavEntry(key = key as AuthorizationScreenRoute) {
        val viewModel = AuthorizationScreenViewModel(
            navigator = navigator,
            authorizer = koinInject()
        )

        AuthorizationScreen(
            viewModel = viewModel,
        )
    }
}