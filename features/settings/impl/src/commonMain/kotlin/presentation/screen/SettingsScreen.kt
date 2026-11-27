package presentation.screen

import AdaptiveLayoutWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import domain.intent.SettingsIntent
import kotlinx.coroutines.flow.SharedFlow
import presentation.actions.SettingsActions
import presentation.effect.SettingsEffect
import presentation.screen.compact.SettingsScreenCompact
import presentation.state.SettingsUiState
import presentation.viewmodel.SettingsScreenViewModel

val LocalSettingsActions = staticCompositionLocalOf<SettingsActions> {
    error("No SettingsActions provided")
}

@Composable
internal fun SettingsScreen(
    viewModel: SettingsScreenViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val effect = viewModel.effect
    
    val actions = SettingsActions(
        logout = { viewModel.sendIntent(SettingsIntent.Logout) }
    )

    CompositionLocalProvider(LocalSettingsActions provides actions) {
        SettingsScreenContentShell(state, effect)
    }
}

@Composable
private fun SettingsScreenContentShell(
    state: SettingsUiState,
    effect: SharedFlow<SettingsEffect>,
) {
    AdaptiveLayoutWrapper(
        effect = effect,
        state = state,
        compact = { state, effect ->
            SettingsScreenCompact(state, effect)
        }
    )
}
