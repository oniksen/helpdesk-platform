package presentation.screen.compact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import helpdesk_platform.features.settings.impl.generated.resources.Res
import helpdesk_platform.features.settings.impl.generated.resources.settings_page_title
import helpdesk_platform.features.settings.impl.generated.resources.settings_undefined
import kotlinx.coroutines.flow.SharedFlow
import org.jetbrains.compose.resources.stringResource
import presentation.effect.SettingsEffect
import presentation.screen.compact.components.appbar.AppBar
import presentation.screen.shared.version.VersionCard
import presentation.state.SettingsUiState

@Composable
fun SettingsScreenCompact(
    state: SettingsUiState,
    effect: SharedFlow<SettingsEffect>,
) {
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effect) {
        effect.collect { effect ->
            snackBarHostState.currentSnackbarData?.dismiss()

            when (effect) {
                is SettingsEffect.ShowSnackBar -> {
                    snackBarHostState.showSnackbar(
                        message = effect.message,
                        withDismissAction = false,
                        duration = SnackbarDuration.Indefinite
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(title = stringResource(resource = Res.string.settings_page_title))
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            VersionCard(
                modifier = Modifier.fillMaxWidth(),
                uiVersion = state.projectUiVersion ?: stringResource(resource = Res.string.settings_undefined)
            )
        }
    }
}
