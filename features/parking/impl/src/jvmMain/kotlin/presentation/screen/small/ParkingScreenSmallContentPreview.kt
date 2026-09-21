package presentation.screen.small

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.ParkingScreenEffect
import presentation.state.ParkingScreenState
import presentation.utils.PreviewWrapper

private val previewState = ParkingScreenState(
    passError = "Ошибка ввода"
)
internal val previewEffect: SharedFlow<ParkingScreenEffect>
    field = MutableSharedFlow<ParkingScreenEffect>(
        replay = 0,
        extraBufferCapacity = 1
    ).apply {
        tryEmit(
            ParkingScreenEffect.ShowSnackBar(
                message = "Ошибка MAX"
            )
        )
    }

@Preview(showSystemUi = true, showBackground = true, name = "Light Mode")
@Composable
private fun ParkingScreenSmallContentPreviewLight() {
    PreviewWrapper {
        ParkingScreenSmallContent(previewState, previewEffect)
    }
}
@Preview(showSystemUi = true, showBackground = true, name = "Light Mode")
@Composable
private fun ParkingScreenSmallContentPreviewDark() {
    PreviewWrapper(true) {
        ParkingScreenSmallContent(previewState, previewEffect)
    }
}