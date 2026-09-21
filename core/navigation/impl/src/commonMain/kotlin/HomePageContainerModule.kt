import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import navigation.TasksPageRoute

class HomePageContainerModule : RootNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TasksPageRoute::class, TasksPageRoute.serializer())
        }
    }

    override fun canResolve(key: NavKey): Boolean = key is TasksPageRoute

    override fun resolve(key: NavKey, navigator: AppNavigator): NavEntry<*> =
        NavEntry(key = key as TasksPageRoute) {
            MainNavigation(startRoute = AppDestination.PARKING.route)
        }
}
