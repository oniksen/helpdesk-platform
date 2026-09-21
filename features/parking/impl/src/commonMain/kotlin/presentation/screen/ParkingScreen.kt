package presentation.screen

import AdaptiveLayoutWrapper
import androidx.compose.runtime.*
import domain.intent.ParkingScreenIntent
import kotlinx.coroutines.flow.SharedFlow
import presentation.actions.ParkingScreenActions
import presentation.effect.ParkingScreenEffect
import presentation.screen.small.ParkingScreenSmallContent
import presentation.state.ParkingScreenState
import presentation.viewmodel.ParkingScreenViewModel

val LocalParkingScreenActions = staticCompositionLocalOf<ParkingScreenActions> {
    error("Local actions not provided")
}

@Composable
fun ParkingScreen(
    viewModel: ParkingScreenViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val effect = viewModel.effect
    val parkingScreenActions = remember {
        ParkingScreenActions(
            openScan = { viewModel.sendIntent(ParkingScreenIntent.OpenScanner) },
            resetScanResult = { viewModel.sendIntent(ParkingScreenIntent.ResetScannerResult) },
            onNumberChanged = { viewModel.sendIntent(ParkingScreenIntent.OnNumberChanged(it)) },
            checkPass = { viewModel.sendIntent(ParkingScreenIntent.CheckPass) },
            getPassDetails = { viewModel.sendIntent(ParkingScreenIntent.GetPassDetails) },
        )
    }

    CompositionLocalProvider(LocalParkingScreenActions provides parkingScreenActions) {
        ParkingScreenContentShell(
            uiState = uiState,
            effect = effect,
        )
    }
}

@Composable
private fun ParkingScreenContentShell(
    uiState: ParkingScreenState,
    effect: SharedFlow<ParkingScreenEffect>,
) {
    AdaptiveLayoutWrapper(
        state = uiState,
        effect = effect,
        compact = { state, effect ->
            ParkingScreenSmallContent(state, effect)
        }
    )
}