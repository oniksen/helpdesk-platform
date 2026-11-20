package presentation.utils

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
internal fun PreviewWrapper(
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(if (darkMode) darkColorScheme() else lightColorScheme()) {
        Surface {
            content()
        }
    }
}