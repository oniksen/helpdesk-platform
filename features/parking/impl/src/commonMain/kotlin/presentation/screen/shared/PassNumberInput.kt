package presentation.screen.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun PassNumberInput(
    number: String,
    error: String? = null,
    onChange: (String) -> Unit,
) {

    OutlinedTextField(
        value = number,
        onValueChange = onChange,
        singleLine = true,
        label = {
            Text(text = "Номер пропуска")
        },
        placeholder = {
            Text("1234567")
        },
        supportingText = {
            if (error != null)
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                )
        },
    )
}