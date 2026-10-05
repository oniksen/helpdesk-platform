package presentation.screen.compact.components.appbar

import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@Composable
internal fun AppBar(
    title: String,
) {
    TopAppBar(
        title = {
            Text(text = title)
        },
    )
}
