package presentation.viewmodel

import AppNavigator
import Authorization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import navigation.HomePageRoute
import domain.intent.AuthorizationScreenIntent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import presentation.state.AuthorizationState
import kotlin.time.Duration.Companion.milliseconds

internal class AuthorizationScreenViewModel(
    private val navigator: AppNavigator,
    private val authorizer: Authorization,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val state: StateFlow<AuthorizationState>
        field = MutableStateFlow(AuthorizationState())

    fun sendIntent(intent: AuthorizationScreenIntent) {
        when(intent) {
            AuthorizationScreenIntent.OpenHomeScreen -> openHomeScreen()
        }
    }

    init {
        scope.launch(Dispatchers.Default) {
            delay(500.milliseconds)
            when(val result = authorizer.getMaxInitData()) {
                is MaxAuthorizationResult.Error -> showAuthorizationError(error = result.error)
                is MaxAuthorizationResult.Success -> makeTestBtnAvailable()
                MaxAuthorizationResult.Unavailable -> notifyMaxAuthNotAvailable()
            }
        }
    }

    private fun openHomeScreen() {
        navigator.navigate(HomePageRoute)
    }
    private fun showAuthorizationError(error: Throwable) {
        updateState { copy(
            authResultMessage = error.message ?: "Ошибка авторизации",
            inProgress = false,
            testBtnEnabled = false,
        ) }
    }
    private fun makeTestBtnAvailable() {
        updateState { copy(
            testBtnEnabled = true,
            authResultMessage = "Данные от Max получены",
            inProgress = false,
        ) }
    }
    private fun notifyMaxAuthNotAvailable() {
        updateState { copy(
            authResultMessage = "MAX авторизация не доступна",
            inProgress = false,
            testBtnEnabled = false,
        )}
    }

    private fun updateState(block: AuthorizationState.() -> AuthorizationState) {
        state.update { state -> block(state) }
    }
}