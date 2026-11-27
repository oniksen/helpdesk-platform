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
        field = MutableStateFlow(
            AuthorizationState(
                loginMode = loginStrategy.mode,
                inProgress = loginStrategy.mode == LoginMode.Silent,
            )
        )
    val effect: SharedFlow<AuthorizationScreenEffect>
        field = MutableSharedFlow<AuthorizationScreenEffect>(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    fun sendIntent(intent: AuthorizationScreenIntent) {
        when(intent) {
            is AuthorizationScreenIntent.Authorize -> authorize(intent.credentials, intent.autoLogin)
        }
    }

    init {
        when (loginStrategy.mode) {
            LoginMode.Silent -> authorize()
            LoginMode.Credentials -> {
                if (authorizer.getAutoLogin())
                    scope.launch {
                        updateState { copy(inProgress = true, authResultMessage = "Восстановление сессии") }

                        val dataRestored = runCatching {
                            withTimeout(RESTORE_TIMEOUT_MS) { authorizer.restoreAuthState() }
                        }.getOrDefault(false)
                        if (dataRestored) {
                            updateState { copy(inProgress = true, authResultMessage = "Сессия восстановлена") }
                            delay(1_000.milliseconds)
                            openHomeScreen()
                        } else {
                            showAuthorizationError(message = "Не удалось восстановить сессию")
                        }
                    }
            }
        }
    }

    private fun authorize(credentials: LoginCredentials? = null, autoLogin: Boolean = false) {
        if (loginStrategy.mode == LoginMode.Credentials) {
            val email = credentials?.email?.trim().orEmpty()
            val password = credentials?.password.orEmpty()
            if (email.isEmpty() || !email.contains("@") || password.isEmpty()) {
                showAuthorizationError(message = "Введите корректный email и пароль")
                return
            }
        }

        updateState { copy(inProgress = true, authResultMessage = null) }
        scope.launch {
            try {
                val authResponse = loginStrategy.authorize(credentials)

                val email = when (loginStrategy.mode) {
                    LoginMode.Silent -> authResponse.email
                    LoginMode.Credentials -> credentials?.email
                } ?: throw AppException.LoginError.IncorrectCredentials(message = "Внутренняя ошибка получения Email")

                val userData = authorizer.fetchUserData(email)
                updateState { copy(
                    inProgress = true,
                    authResultMessage = "Сохранение полученных данных",
                ) }

                authorizer.run {
                    saveAuthData(AuthData(
                        token = authResponse.bearerToken,
                        cookies = authResponse.cookies,
                    ))
                    saveAutoLogin(autoLogin)
                    saveUser(userData)
                    if (autoLogin) {
                        persistAuthState()
                    } else {
                        clearAuthState()
                    }
                }

                delay(1_000.milliseconds)
                openHomeScreen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showAuthorizationError(message = e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun openHomeScreen() {
        // Индикатор держим до самого перехода, иначе на кадр появляется пустая форма входа.
        updateState { copy(authResultMessage = "Успешная авторизация") }

        scope.launch {
            navigator.navigateAndClearBackStack(TasksPageRoute)
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

    private companion object {
        const val RESTORE_TIMEOUT_MS = 10_000L
    }
}