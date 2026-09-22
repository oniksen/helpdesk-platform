package presentation.screen.compact

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.UpdateScreenEffect
import presentation.screen.LocalUpdateActions
import presentation.state.UpdateState
import presentation.state.UpdateStatus

@Composable
internal fun UpdateScreenCompact(
    state: UpdateState,
    effect: SharedFlow<UpdateScreenEffect>,
) {
    val actions = LocalUpdateActions.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(effect) {
        effect.collect { effect ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (effect) {
                is UpdateScreenEffect.ShowSnackBar -> {
                    val result = snackbarHostState.showSnackbar(
                        withDismissAction = false,
                        duration = SnackbarDuration.Indefinite,
                        message = effect.message,
                        actionLabel = "Повторить",
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> { }
                        SnackbarResult.ActionPerformed -> actions.startUpdate()
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Обновление приложения",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = currentState.inProgress
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularWavyProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentState.message ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = !currentState.inProgress && currentState.message != null,
                ) {
                    SelectionContainer {
                        Text(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            text = currentState.message ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
