package presentation.screen.shared.buttons

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import helpdesk_platform.features.settings.impl.generated.resources.Res
import helpdesk_platform.features.settings.impl.generated.resources.settings_logout_btn
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LogoutButton(onClick: () -> Unit) {
    Button(
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        onClick = onClick,
    ) {
        Text(
            text = stringResource(Res.string.settings_logout_btn),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
