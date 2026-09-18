package presentation.screen

import AdaptiveLayoutWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import domain.intent.TasksPageIntent
import domain.model.TasksPage
import kotlinx.coroutines.flow.SharedFlow
import presentation.actions.TasksPageActions
import presentation.effect.TasksPageEffect
import presentation.screen.compact.TasksPageContentCompact
import presentation.state.TasksPageState
import presentation.viewmodel.TasksPageViewModel

val LocalTasksPageActions = staticCompositionLocalOf<TasksPageActions> {
    error("No actions provided")
}

@Composable
internal fun TasksPage(
    tasksPageViewModel: TasksPageViewModel,
) {
    val uiState by tasksPageViewModel.uiState.collectAsState()
    val effect = tasksPageViewModel.effect
    val actions = remember {
        TasksPageActions(
            openDetailsPage = { tasksPageViewModel.sendIntent(TasksPageIntent.OpenDetailsPage(it)) },
            changeFirstVisibleIndex = { tasksPageViewModel.sendIntent(TasksPageIntent.FirstVisibleIndexChanged(it)) },
            cancelJobs = { tasksPageViewModel.sendIntent(TasksPageIntent.CancelJobs) }
        )
    }

    CompositionLocalProvider(LocalTasksPageActions provides actions) {
        TasksPageContentShell(
            state = uiState,
            effect = effect,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            actions.cancelJobs()
        }
    }
}

@Composable
internal fun TasksPageContentShell(
    state: TasksPageState,
    effect: SharedFlow<TasksPageEffect>,
) {
    AdaptiveLayoutWrapper(
        state = state,
        effect = effect,
        compact = { state, effect ->
            TasksPageContentCompact(state, effect)
        }
    )
}

