import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun RootNavigation(startRoute: NavKey) {
    KoinContext {
        val koin = getKoin()

        // 1. Собираем все зарегистрированные модули рутовой навигации.
        val rootModules = remember { koin.getAll<RootNavModule>() }

        // 2. Динамическая сборка конфигурации сериализации всех рутовых маршрутов.
        val rootNavConfig = remember(rootModules) {
            val combinedSerializer = rootModules
                .map { it.serializerModule }
                .reduceOrNull { acc, module -> acc + module } ?: SerializersModule { }

            SavedStateConfiguration { serializersModule = combinedSerializer }
        }

        // 3. Инициализация навигационного стека.
        val navBackStack = rememberNavBackStack(rootNavConfig, startRoute)

        // 4. Реализация навигатора по маршрутам.
        val navigator = remember(navBackStack) {
            object : AppNavigator {
                override fun navigate(route: NavKey) {
                    navBackStack.add(route)
                }
                override fun popBackStack() {
                    if (navBackStack.size > 1) navBackStack.removeLast()
                }
            }
        }

        NavDisplay(
            backStack = navBackStack,
        ) { key ->
            val module = rootModules.firstOrNull { it.canResolve(key) }
                ?: error("Не найден навигационный модуль для маршрута $key")

            module.resolve(key = key, navigator = navigator) as NavEntry<NavKey>
        }
    }
}