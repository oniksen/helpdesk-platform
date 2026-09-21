package presentation.screen.compact

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.AuthorizationScreenEffect
import presentation.state.AuthorizationState
import presentation.utils.PreviewWrapper

private val PreviewState = AuthorizationState()
internal val PreviewEffect: SharedFlow<AuthorizationScreenEffect>
    field = MutableSharedFlow<AuthorizationScreenEffect>(
        replay = 0,
        extraBufferCapacity = 1
    ).apply {
        tryEmit(
            AuthorizationScreenEffect.ShowSnackBar(
                message = "Ошибка Авторизации"
            )
        )
    }

@Preview
@Composable
fun AuthorizationScreenCompactPreview() {
    PreviewWrapper {
        AuthorizationScreenCompact(
            state = PreviewState,
            effect = PreviewEffect
        )
    }
}