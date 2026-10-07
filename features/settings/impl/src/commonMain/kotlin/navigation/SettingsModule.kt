package navigation

import AppNavigator
import FeatureNavModule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import presentation.screen.SettingsScreen
import presentation.viewmodel.SettingsScreenViewModel

class SettingsModule: FeatureNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(NavKey::class) { subclass(SettingsScreenRoute::class, SettingsScreenRoute.serializer()) }
    }

    override fun canResolve(key: NavKey): Boolean = key is SettingsScreenRoute

    override fun resolve(
        key: NavKey,
        navigator: AppNavigator
    ): NavEntry<out NavKey> = NavEntry(key = key as SettingsScreenRoute) {
        val settingsScreenViewModel = SettingsScreenViewModel(
            appVersionProvider = koinInject(),
            currentChannelProvider = koinInject(),
        )

        SettingsScreen(
            viewModel = settingsScreenViewModel,
        )
    }
}
