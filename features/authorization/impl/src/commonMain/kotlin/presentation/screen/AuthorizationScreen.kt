package presentation.screen

import AdaptiveLayoutWrapper
import androidx.compose.runtime.*
import domain.intent.AuthorizationScreenIntent
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.AuthorizationScreenEffect
import presentation.actions.AuthorizationScreenActions
import presentation.screen.compact.AuthorizationScreenCompact
import presentation.state.AuthorizationState
import presentation.viewmodel.AuthorizationScreenViewModel

val LocalAuthorizationActions = staticCompositionLocalOf<AuthorizationScreenActions> {
    error("No AuthorizationScreenActions provided")
}

@Composable
internal fun AuthorizationScreen(
    viewModel: AuthorizationScreenViewModel,
) {
    val state by viewModel.state.collectAsState()
    val effect = viewModel.effect
    val actions = AuthorizationScreenActions(
        authorize = { viewModel.sendIntent(AuthorizationScreenIntent.Authorize) }
    )

    CompositionLocalProvider(LocalAuthorizationActions provides actions) {
        AuthorizationScreenShell(
            state = state,
            effect = effect
        )
    }
}

@Composable
private fun AuthorizationScreenShell(
    state: AuthorizationState,
    effect: SharedFlow<AuthorizationScreenEffect>,
) {
    AdaptiveLayoutWrapper(
        state = state,
        effect = effect,
        compact = { state, effect -> AuthorizationScreenCompact(state, effect) }
    )
}