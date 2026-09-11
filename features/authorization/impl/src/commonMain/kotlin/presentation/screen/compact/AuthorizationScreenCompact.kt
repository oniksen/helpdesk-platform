package presentation.screen.compact

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import presentation.AuthorizationScreenEffect
import presentation.screen.LocalAuthorizationActions
import presentation.state.AuthorizationState

@Composable
internal fun AuthorizationScreenCompact(
    state: AuthorizationState,
    effect: SharedFlow<AuthorizationScreenEffect>,
) {
    val actions = LocalAuthorizationActions.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effect) {
        effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is AuthorizationScreenEffect.ShowSnackBar -> {
                    val result = snackbarHostState.showSnackbar(
                        withDismissAction = false,
                        duration = SnackbarDuration.Indefinite,
                        message = effect.message,
                        actionLabel = "Повторить"
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> { }
                        SnackbarResult.ActionPerformed -> actions.authorize()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = state,
        ) { currentState ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
            }
        }
    }
}