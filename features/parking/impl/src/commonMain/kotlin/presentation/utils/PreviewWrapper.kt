package presentation.utils

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import presentation.actions.ParkingScreenActions
import presentation.screen.LocalParkingScreenActions

@Composable
internal fun PreviewWrapper(
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val parkingScreenActions = ParkingScreenActions(
        openScan = { },
        resetScanResult = { },
        onNumberChanged = { },
        checkPass = { },
        getPassDetails = { },
    )

    CompositionLocalProvider(LocalParkingScreenActions provides parkingScreenActions) {
        MaterialExpressiveTheme(if (darkMode) darkColorScheme() else lightColorScheme()) {
            Surface {
                content()
            }
        }
    }
}