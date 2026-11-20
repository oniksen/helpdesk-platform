package presentation.screen

import AdaptiveLayoutWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.SettingsEffect
import presentation.screen.compact.SettingsScreenCompact
import presentation.state.SettingsUiState
import presentation.viewmodel.SettingsScreenViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val effect = viewModel.effect

    SettingsScreenContentShell(state, effect)
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
