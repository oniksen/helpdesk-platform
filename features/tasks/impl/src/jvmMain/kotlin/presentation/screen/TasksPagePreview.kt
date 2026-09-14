package presentation.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
private fun PreviewState() {
    HomePageContent(
        onParkingPage = { }
    )
}

@Preview
@Composable
private fun HomePagePreviewLight() {
    MaterialTheme {
        Surface {
            PreviewState()
        }
    }
}

@Preview
@Composable
private fun HomePagePreviewDark() {
    MaterialTheme(darkColorScheme()) {
        Surface {
            PreviewState()
        }
    }
}