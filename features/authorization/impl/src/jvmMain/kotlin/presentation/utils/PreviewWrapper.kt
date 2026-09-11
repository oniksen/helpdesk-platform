package presentation.utils

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import presentation.actions.AuthorizationScreenActions
import presentation.screen.LocalAuthorizationActions

@Composable
internal fun PreviewWrapper(
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val parkingScreenActions = AuthorizationScreenActions(
        authorize = { }
    )

    CompositionLocalProvider(LocalAuthorizationActions provides parkingScreenActions) {
        MaterialExpressiveTheme(if (darkMode) darkColorScheme() else lightColorScheme()) {
            Surface {
                content()
            }
        }
    }
}