import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import org.koin.compose.KoinContext
import org.koin.compose.getKoin

@Composable
fun MainNavigation(startRoute: NavKey) {
    KoinContext {
        val koin = getKoin()

        val featureModules = remember { koin.getAll<FeatureNavModule>() }

        val appNavConfig = remember(featureModules) {
            val combinedSerializer = featureModules
                .map { it.serializerModule }
                .reduceOrNull { acc, module -> acc + module } ?: SerializersModule { }

            SavedStateConfiguration { serializersModule = combinedSerializer }
        }

        val parkingBackStack = rememberNavBackStack(appNavConfig, AppDestination.PARKING.route)
        val tasksBackStack = rememberNavBackStack(appNavConfig, AppDestination.TASKS.route)

        val initialTab = remember(startRoute) {
            AppDestination.entries.firstOrNull { it.route == startRoute } ?: AppDestination.PARKING
        }
        var currentTab by remember { mutableStateOf(initialTab) }

        val activeBackStack = when (currentTab) {
            AppDestination.PARKING -> parkingBackStack
            AppDestination.TASKS -> tasksBackStack
        }

        val navigator = remember(activeBackStack) {
            object : AppNavigator {
                override fun navigate(route: NavKey) {
                    activeBackStack.add(route)
                }

                override fun popBackStack() {
                    if (activeBackStack.size > 1) activeBackStack.removeLast()
                }
            }
        }

        AdaptiveNavigationContainer(
            currentDestination = currentTab,
            onDestinationChanged = { destination -> currentTab = destination },
        ) {
            NavDisplay(
                backStack = activeBackStack,
            ) { key ->
                val module = featureModules.firstOrNull { it.canResolve(key) }
                    ?: error("Не найден навигационный модуль для маршрута $key")

                module.resolve(key = key, navigator = navigator) as NavEntry<NavKey>
            }
        }
    }
}
