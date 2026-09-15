package presentation.screen.compact

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import presentation.actions.TasksPageActions
import presentation.screen.LocalTasksPageActions

@Composable
internal fun PreviewWrapper(dark: Boolean = false, content: @Composable () -> Unit) {
    val actions = TasksPageActions(
        openDetailsPage = {

        }
    )
    CompositionLocalProvider(LocalTasksPageActions provides actions) {
        MaterialExpressiveTheme(
            colorScheme = if (dark) darkColorScheme() else lightColorScheme()
        ) {
            Surface {
                content()
            }
        }
    }
}