package presentation.viewmodel

import QrCodeScanner
import QrCodeScannerResult
import domain.intent.ParkingScreenIntent
import domain.model.toPassNumber
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import max_helpdesk.features.parking.impl.generated.resources.Res
import max_helpdesk.features.parking.impl.generated.resources.parking_scan_max_unavailable
import org.jetbrains.compose.resources.getString
import presentation.effect.ParkingScreenEffect
import presentation.state.ParkingScreenState
import presentation.state.PassInputState

class ParkingScreenViewModel(
    private val qrCodeScanner: QrCodeScanner,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val uiState: StateFlow<ParkingScreenState>
        field = MutableStateFlow(ParkingScreenState())
    val effect: SharedFlow<ParkingScreenEffect>
        field = MutableSharedFlow<ParkingScreenEffect>(
            replay = 0, // Не храним и не перевыпускаем старые эффекты при пересоздании экрана
            extraBufferCapacity = 1,    // Позволяет делать tryEmit() без блокировки потока
            onBufferOverflow = BufferOverflow.DROP_OLDEST,  // Если буфер забит, старое событие стирается, новое доставляется
        )

    fun sendIntent(intent: ParkingScreenIntent) {
        when (intent) {
            is ParkingScreenIntent.OpenScanner -> openScanner(fileSelect = true)
            is ParkingScreenIntent.ResetScannerResult -> resetScannerResult()
            is ParkingScreenIntent.OnNumberChanged -> updateInputNumber(intent.number)
            is ParkingScreenIntent.CheckPass -> checkInputPass()
            is ParkingScreenIntent.GetPassDetails -> openPassDetailsScreen()
        }
    }

    private fun openScanner(fileSelect: Boolean) {
        scope.launch(Dispatchers.Default) {
            val result = qrCodeScanner.scan(fileSelect = fileSelect)

            withContext(Dispatchers.Main) {
                when (result) {
                    is QrCodeScannerResult.Error -> {
                        updateState { copy(passError = result.cause.message) }
                    }
                    is QrCodeScannerResult.Success -> {
                        result.value.toPassNumber()
                            .onSuccess {
                                updateState { copy(passError = null, passNumber = it.number) }
                            }
                            .onFailure {
                                updateState { copy(passError = it.message) }
                            }
                    }
                    is QrCodeScannerResult.Unavailable -> {
                        resetScannerResult()
                        // Вызвать Snackbar.
                        effect.tryEmit(
                            value = ParkingScreenEffect.ShowSnackBar(
                                message = getString(Res.string.parking_scan_max_unavailable)
                            )
                        )
                    }
                }
            }
        }
    }
    /** Сброс состояний связанных с номером пропуска до дефолтных. */
    private fun resetScannerResult() {
        updateState { copy(
            passError = null,
            passNumber = "",
        ) }
    }
    private fun updateInputNumber(number: String) {
        if (number.length > PASS_NUMBER_MAX_LENGTH) return

        updateState { copy(passNumber = number) }
    }
    private fun checkInputPass() {
        val passSnapshot = uiState.value.passNumber
        validatePassNumber(passSnapshot)
    }
    private fun validatePassNumber(number: String) {
        scope.launch(Dispatchers.Main) {
            updateState { copy(passInputState = PassInputState.Checking) }

            val passNumber = withContext(Dispatchers.Default) { number.toPassNumber() }

            passNumber
                .onSuccess {
                    updateState {
                        copy(
                            passError = null,
                            passInputState = PassInputState.Success
                        )
                    }
                }
                .onFailure {
                    updateState {
                        copy(
                            passError = it.message,
                            passInputState = PassInputState.Idle
                        )
                    }
                }
        }
    }
    private fun openPassDetailsScreen() {
        scope.launch(Dispatchers.Main) {
            effect.emit(ParkingScreenEffect.ShowSnackBar(
                message = "В разработке"
            ))
        }
    }

    private fun updateState(block: ParkingScreenState.() -> ParkingScreenState) =
        uiState.update { state -> block(state) }

    private companion object {
        const val PASS_NUMBER_MAX_LENGTH = 9
    }
}