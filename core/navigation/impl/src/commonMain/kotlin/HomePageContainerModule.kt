import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import navigation.HomePageRoute

class HomePageContainerModule : RootNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(HomePageRoute::class, HomePageRoute.serializer())
        }
    }

    override fun canResolve(key: NavKey): Boolean = key is HomePageRoute

    override fun resolve(key: NavKey, navigator: AppNavigator): NavEntry<*> =
        NavEntry(key = key as HomePageRoute) {
            MainNavigation(startRoute = AppDestination.PARKING.route)
        }
}
