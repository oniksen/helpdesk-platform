package navigation

import AppNavigator
import FeatureNavModule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import presentation.screen.HomePage
import presentation.viewmodel.TasksPageViewModel

class TasksPageModule : FeatureNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(NavKey::class) { subclass(TasksPageRoute::class, TasksPageRoute.serializer()) }
    }

    override fun canResolve(key: NavKey): Boolean = key is TasksPageRoute

    override fun resolve(
        key: NavKey,
        navigator: AppNavigator,
    ): NavEntry<out NavKey> = NavEntry(key = key as TasksPageRoute) {
        val tasksPageViewModel = TasksPageViewModel(
            navigator = navigator,
        )

        HomePage(
            tasksPageViewModel = tasksPageViewModel,
        )
    }
}