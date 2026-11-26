package presentation.viewmodel

import AppException
import AppNavigator
import Authorization
import auth.AuthData
import domain.intent.AuthorizationScreenIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import login.LoginCredentials
import login.LoginMode
import login.LoginStrategy
import navigation.TasksPageRoute
import presentation.effect.AuthorizationScreenEffect
import presentation.state.AuthorizationState
import kotlin.time.Duration.Companion.milliseconds

internal class AuthorizationScreenViewModel(
    private val navigator: AppNavigator,
    private val authorizer: Authorization,
    private val loginStrategy: LoginStrategy,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val state: StateFlow<AuthorizationState>
        field = MutableStateFlow(AuthorizationState(loginMode = loginStrategy.mode))
    val effect: SharedFlow<AuthorizationScreenEffect>
        field = MutableSharedFlow<AuthorizationScreenEffect>(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    fun sendIntent(intent: AuthorizationScreenIntent) {
        when(intent) {
            is AuthorizationScreenIntent.Authorize -> authorize(intent.credentials)
        }
    }

    init {
        if (loginStrategy.mode == LoginMode.Silent) authorize()
    }

    private fun authorize(credentials: LoginCredentials? = null) {
        scope.launch(Dispatchers.Default) {
            try {
                val authResponse = loginStrategy.authorize(credentials)
                val userData = authorizer.fetchUserData(authResponse.email)
                updateState { copy(
                    inProgress = true,
                    authResultMessage = "Сохранение полученных данных",
                ) }
                authorizer.saveAuthData(AuthData(
                    token = authResponse.bearerToken,
                    cookies = authResponse.cookies,
                ))
                authorizer.saveUser(userData)
                authorizer.persistAuthState()

                delay(1_000.milliseconds)
                openHomeScreen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException.LoginError) {
                showAuthorizationError(message = e.message ?: "Неизвестная ошибка")
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
            navigator.navigate(TasksPageRoute)
        }
    }
    private fun showAuthorizationError(message: String) {
        effect.tryEmit(AuthorizationScreenEffect.ShowSnackBar(message))

        updateState { copy(
            authResultMessage = "Ошибка авторизации. Повторите снова",
            inProgress = false,
        ) }
    }

    private fun updateState(block: AuthorizationState.() -> AuthorizationState) {
        state.update { state -> block(state) }
    }
}