package presentation.screen.compact

import AppVersion
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableSharedFlow
import presentation.effect.SettingsEffect
import presentation.state.SettingsUiState
import presentation.utils.PreviewWrapper

private val appVersion = AppVersion(0, 3, 0, AppVersion.Stage.Alpha, 4)

private val uiState = SettingsUiState(
    projectUiVersion = appVersion.toString(),
    currentChannel = "dev.canary"
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
