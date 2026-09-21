package presentation.viewmodel

import AppNavigator
import AuthResult
import Authorization
import MaxAuthorizationResult
import auth.AuthData
import domain.intent.AuthorizationScreenIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import navigation.TasksPageRoute
import presentation.effect.AuthorizationScreenEffect
import presentation.state.AuthorizationState
import user.UserData
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
            AuthorizationScreenIntent.Authorize -> authorization()
        }
    }

    init {
        authorization()
    }

    private fun authorization() {
        updateState { copy(
            inProgress = true,
            authResultMessage = null,
        ) }

        scope.launch(Dispatchers.Default) {
            try {
                getMaxInitDataOrShowError()?.let { maxInitData ->
                    val authResponse = authorizer.helpdeskAuth(maxInitData)
                    val userData = authorizer.fetchUserData(authResponse.email)
                    updateState { copy(
                        inProgress = true,
                        authResultMessage = "Сохранение полученных данных",
                    ) }
                    authorizer.saveAuthData(AuthData(
                        maxInitData = maxInitData,
                        token = authResponse.bearerToken,
                        cookies = authResponse.cookies,
                    ))
                    authorizer.saveUser(userData)

                    delay(1_000.milliseconds)
                    openHomeScreen()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showAuthorizationError(e.message ?: "Ошибка авторизации")
            }
        }
    }

    private suspend fun getMaxInitDataOrShowError(): String? {
        when(val result = authorizer.getMaxInitData()) {
            MaxAuthorizationResult.Unavailable -> notifyMaxAuthNotAvailable()
            is MaxAuthorizationResult.Success -> return result.initData
            is MaxAuthorizationResult.Error ->
                showAuthorizationError(message = result.error.message ?: "Ошибка авторизации")
        }
        return null
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
    private fun notifyMaxAuthNotAvailable() {
        effect.tryEmit(AuthorizationScreenEffect.ShowSnackBar("MAX авторизация не доступна"))

        updateState { copy(
            authResultMessage = null,
            inProgress = false,
        )}
    }

    private fun updateState(block: AuthorizationState.() -> AuthorizationState) {

        state.update { state -> block(state) }
    }
}