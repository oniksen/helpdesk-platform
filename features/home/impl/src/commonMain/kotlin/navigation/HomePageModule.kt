package navigation

import AppNavigator
import FeatureNavModule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import presentation.screen.HomePage
import presentation.viewmodel.HomePageViewModel

class HomePageModule : FeatureNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(NavKey::class) { subclass(HomePageRoute::class, HomePageRoute.serializer()) }
    }

    override fun canResolve(key: NavKey): Boolean = key is HomePageRoute

    override fun resolve(
        key: NavKey,
        navigator: AppNavigator,
    ): NavEntry<out NavKey> = NavEntry(key = key as HomePageRoute) {
        val homePageViewModel = HomePageViewModel(
            navigator = navigator,
        )

        HomePage(
            homePageViewModel = homePageViewModel,
        )
    }
}