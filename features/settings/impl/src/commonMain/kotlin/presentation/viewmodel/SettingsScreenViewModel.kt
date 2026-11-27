package presentation.viewmodel

import AppNavigator
import AppVersionProvider
import AuthSession
import AuthorizationScreenRoute
import BaseViewModel
import CurrentChannelProvider
import domain.intent.SettingsIntent
import kotlinx.coroutines.launch
import login.LoginMode
import login.LoginStrategy
import presentation.effect.SettingsEffect
import presentation.state.SettingsUiState

internal class SettingsScreenViewModel(
    appVersionProvider: AppVersionProvider,
    currentChannelProvider: CurrentChannelProvider,
    private val navigator: AppNavigator,
    private val loginStrategy: LoginStrategy,
    private val authSession: AuthSession,
): BaseViewModel<SettingsUiState, SettingsEffect>(SettingsUiState()) {
    init {
        val appVersion = appVersionProvider.provide()
        val currentChannel = currentChannelProvider.provide()

        updateState { copy(
            projectUiVersion = appVersion.toString(),
            currentChannel = currentChannel,
            exitButtonAvailable = loginStrategy.mode == LoginMode.Credentials,
        ) }
    }

    fun sendIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Logout -> if (uiState.value.exitButtonAvailable) logout()
        }
    }

    private fun logout() {
        scope.launch {
            authSession.clearAuthState()
            navigator.navigateAndClearBackStack(AuthorizationScreenRoute)
        }
    }
}
