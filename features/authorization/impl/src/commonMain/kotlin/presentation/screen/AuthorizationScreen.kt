package presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.intent.AuthorizationScreenIntent
import presentation.actions.AuthorizationScreenActions
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
    val actions = AuthorizationScreenActions(
        openHomePage = { viewModel.sendIntent(AuthorizationScreenIntent.OpenHomeScreen) }
    )

    CompositionLocalProvider(LocalAuthorizationActions provides actions) {
        AuthorizationScreenContent(
            state = state,
        )
    }
}

@Composable
private fun AuthorizationScreenContent(
    state: AuthorizationState,
) {
    val actions = LocalAuthorizationActions.current

    AnimatedContent(
        targetState = state,
    ) { currentState ->
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Авторизация",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = currentState.inProgress
            ) {
                CircularWavyProgressIndicator()
                Spacer(modifier = Modifier.height(4.dp))
            }
            AnimatedVisibility(
                visible = currentState.authResultMessage != null,
            ) {
                Text(
                    text = currentState.authResultMessage ?: "",
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Button(
                enabled = currentState.testBtnEnabled,
                onClick = actions.openHomePage,
            ) {
                Text(
                    text = "Продолжить"
                )
            }
        }
    }
}