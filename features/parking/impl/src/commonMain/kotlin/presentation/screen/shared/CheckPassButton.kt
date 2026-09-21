package presentation.screen.shared

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import max_helpdesk.features.parking.impl.generated.resources.Res
import max_helpdesk.features.parking.impl.generated.resources.parking_check_pass_btn_next_step
import max_helpdesk.features.parking.impl.generated.resources.parking_check_pass_btn_process
import max_helpdesk.features.parking.impl.generated.resources.parking_check_pass_btn_text_default
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import presentation.state.PassInputState

private data class CheckPassButtonState(
    val isEnabled: Boolean,
    val text: StringResource
)

@Composable
internal fun CheckPassButton(
    passInputState: PassInputState,
    onClick: () -> Unit
) {
    val buttonState: CheckPassButtonState = when (passInputState) {
        PassInputState.Idle -> CheckPassButtonState(true, Res.string.parking_check_pass_btn_text_default)
        PassInputState.Checking -> CheckPassButtonState(false, Res.string.parking_check_pass_btn_process)
        PassInputState.Success -> CheckPassButtonState(true, Res.string.parking_check_pass_btn_next_step)
    }

    Button(
        enabled = buttonState.isEnabled,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(resource = buttonState.text),
        )
    }
}