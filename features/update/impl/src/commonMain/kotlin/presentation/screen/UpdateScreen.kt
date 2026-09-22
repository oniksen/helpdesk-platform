package presentation.screen

import AdaptiveLayoutWrapper
import androidx.compose.runtime.*
import presentation.intent.UpdateScreenIntent
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.UpdateScreenEffect
import presentation.actions.UpdateScreenActions
import presentation.screen.compact.UpdateScreenCompact
import presentation.state.UpdateState
import presentation.viewmodel.UpdateScreenViewModel

val LocalUpdateActions = staticCompositionLocalOf<UpdateScreenActions> {
    error("No UpdateScreenActions provided")
}

@Composable
internal fun UpdateScreen(
    viewModel: UpdateScreenViewModel,
) {
    val state by viewModel.state.collectAsState()
    val effect = viewModel.effect
    val actions = UpdateScreenActions(
        startUpdate = { viewModel.sendIntent(UpdateScreenIntent.StartUpdate) }
    )

    CompositionLocalProvider(LocalUpdateActions provides actions) {
        UpdateScreenShell(
            state = state,
            effect = effect
        )
    }
}

@Composable
private fun UpdateScreenShell(
    state: UpdateState,
    effect: SharedFlow<UpdateScreenEffect>,
) {
    AdaptiveLayoutWrapper(
        state = state,
        effect = effect,
        compact = { state, effect -> UpdateScreenCompact(state, effect) }
    )
}
