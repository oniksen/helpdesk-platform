package presentation.screen.small

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import presentation.actions.ParkingScreenActions
import presentation.effect.ParkingScreenEffect
import presentation.screen.LocalParkingScreenActions
import presentation.screen.shared.CheckPassButton
import presentation.screen.shared.PassNumberInput
import presentation.state.ParkingScreenState
import presentation.state.PassInputState

@Composable
internal fun ParkingScreenSmallContent(
    state: ParkingScreenState,
    effect: SharedFlow<ParkingScreenEffect>,
) {
    val localParkingScreenActions = LocalParkingScreenActions.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effect) {
        effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()

            when (effect) {
                is ParkingScreenEffect.ShowSnackBar -> {
                    snackbarHostState.showSnackbar(
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite,
                        message = effect.message,
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PassField(
                    state = state,
                    localParkingScreenActions = localParkingScreenActions,
                )
                CheckPassButton(
                    passInputState = state.passInputState,
                    onClick = {
                        when (state.passInputState) {
                            PassInputState.Idle -> localParkingScreenActions.checkPass()
                            PassInputState.Success -> localParkingScreenActions.getPassDetails()
                            else -> { }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PassField(
    state: ParkingScreenState,
    localParkingScreenActions: ParkingScreenActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        PassNumberInput(
            number = state.passNumber,
            error = state.passError,
            onChange = localParkingScreenActions.onNumberChanged,
        )
        Spacer(modifier = Modifier.width(4.dp))
        ScanQrButton(
            modifier = Modifier
                .height(60.dp)
                .offset(y = (-4).dp)
        ) {
            localParkingScreenActions.openScan()
        }
    }
}

@Composable
private fun ScanQrButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledIconButton(
        modifier = modifier,
        colors = IconButtonDefaults
            .iconButtonColors()
            .copy(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCodeScanner,
            contentDescription = null,
        )
    }
}
