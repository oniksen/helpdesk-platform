package presentation.viewmodel

import AppNavigator
import AuthResponse
import Authorization
import MaxAuthorizationResult
import domain.intent.AuthorizationScreenIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import navigation.HomePageRoute
import presentation.AuthorizationScreenEffect
import presentation.state.AuthorizationState
import kotlin.time.Duration.Companion.milliseconds

internal class AuthorizationScreenViewModel(
    private val navigator: AppNavigator,
    private val authorizer: Authorization,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val state: StateFlow<AuthorizationState>
        field = MutableStateFlow(AuthorizationState())
    val effect: SharedFlow<AuthorizationScreenEffect>
        field = MutableSharedFlow<AuthorizationScreenEffect>(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    fun sendIntent(intent: AuthorizationScreenIntent) {
        when(intent) {
            AuthorizationScreenIntent.Authorize -> authUser()
        }
    }

    init {
        authUser()
    }

    private fun authUser() {
        updateState { copy(
            inProgress = true,
            authResultMessage = null,
        ) }

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
        updateState { copy(
            authResultMessage = "Успешная авторизация",
            inProgress = false,
        ) }

        scope.launch {
            delay(1_000.milliseconds)
            navigator.navigate(HomePageRoute)
        }
    }
    private fun showAuthorizationError(message: String) {
        effect.tryEmit(AuthorizationScreenEffect.ShowSnackBar(message))

        updateState { copy(
            authResultMessage = null,
            inProgress = false,
        ) }
    }
    private fun notifyMaxAuthNotAvailable() {
        effect.tryEmit(AuthorizationScreenEffect.ShowSnackBar("MAX авторизация не доступна"))

        updateState { copy(
            authResultMessage = null,
            inProgress = false,
        )}
    }
    private fun helpdeskAuth(initData: String) {
        scope.launch(Dispatchers.Default) {
            try {
                when (val result = authorizer.helpdeskAuth(initData)) {
                    is AuthResponse.Error -> showAuthorizationError(result.message)
                    is AuthResponse.Success -> openHomeScreen()
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