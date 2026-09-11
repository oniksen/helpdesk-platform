package presentation.screen.compact

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import max_helpdesk.features.authorization.impl.generated.resources.Res
import max_helpdesk.features.authorization.impl.generated.resources.auth_repeat_action
import max_helpdesk.features.authorization.impl.generated.resources.auth_screen_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
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
                        actionLabel = getString(resource = Res.string.auth_repeat_action)
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
            modifier = Modifier.padding(innerPadding),
            targetState = state,
        ) { currentState ->
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(resource = Res.string.auth_screen_title),
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