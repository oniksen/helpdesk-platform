package navigation

import AppNavigator
import UpdateScreenRoute
import RootNavModule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import presentation.screen.UpdateScreen
import presentation.viewmodel.UpdateScreenViewModel

class UpdateScreenModule : RootNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(baseClass = NavKey::class) {
            subclass(UpdateScreenRoute::class, UpdateScreenRoute.serializer())
        }
    }

    override fun canResolve(key: NavKey): Boolean = key is UpdateScreenRoute

    override fun resolve(
        key: NavKey,
        navigator: AppNavigator
    ): NavEntry<*> = NavEntry(key = key as UpdateScreenRoute) {
        val viewModel = UpdateScreenViewModel(
            navigator = navigator,
            updater = koinInject()
        )

        UpdateScreen(
            viewModel = viewModel,
        )
    }
}
