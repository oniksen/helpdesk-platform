package presentation.screen.compact

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableSharedFlow
import presentation.effect.SettingsEffect
import presentation.state.SettingsUiState
import presentation.utils.PreviewWrapper

private val uiState = SettingsUiState(
    projectUiVersion = "1.0.0-alpha.3",
)
private val effect = MutableSharedFlow<SettingsEffect>()
    .apply {
        tryEmit(
            SettingsEffect.ShowSnackBar(
                message = "Show snack bar",
            )
        )
    }

@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun SettingsScreenCompactPreview() {
    PreviewWrapper {
        SettingsScreenCompact(uiState, effect)
    }
}
