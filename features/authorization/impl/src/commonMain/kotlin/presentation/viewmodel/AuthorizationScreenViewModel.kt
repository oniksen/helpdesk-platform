package presentation.viewmodel

import AppNavigator
import AuthResponse
import Authorization
import MaxAuthorizationResult
import domain.intent.AuthorizationScreenIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import navigation.HomePageRoute
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
            try {
                when(val result = authorizer.getMaxInitData()) {
                    is MaxAuthorizationResult.Error ->
                        showAuthorizationError(message = result.error.message ?: "Ошибка авторизации")
                    is MaxAuthorizationResult.Success -> helpdeskAuth(initData = result.initData)
                    MaxAuthorizationResult.Unavailable -> notifyMaxAuthNotAvailable()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showAuthorizationError(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun openHomeScreen() {
        navigator.navigate(HomePageRoute)
    }
    private fun showAuthorizationError(message: String) {
        updateState { copy(
            authResultMessage = message,
            inProgress = false,
            testBtnEnabled = false,
        ) }
    }
    private fun makeTestBtnAvailable() {
        updateState { copy(
            testBtnEnabled = true,
            authResultMessage = "Успешная авторизация на Helpdesk",
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
    private fun helpdeskAuth(initData: String) {
        scope.launch(Dispatchers.Default) {
            try {
                when (val result = authorizer.helpdeskAuth(initData)) {
                    is AuthResponse.Error -> showAuthorizationError(result.message)
                    is AuthResponse.Success -> makeTestBtnAvailable()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showAuthorizationError(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun updateState(block: AuthorizationState.() -> AuthorizationState) {

        state.update { state -> block(state) }
    }
}