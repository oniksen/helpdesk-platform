package presentation.viewmodel

import AppVersionProvider
import BaseViewModel
import CurrentChannelProvider
import presentation.effect.SettingsEffect
import presentation.state.SettingsUiState

class SettingsScreenViewModel(
    appVersionProvider: AppVersionProvider,
    currentChannelProvider: CurrentChannelProvider,
): BaseViewModel<SettingsUiState, SettingsEffect>(SettingsUiState()) {
    init {
        val appVersion = appVersionProvider.provide()
        val currentChannel = currentChannelProvider.provide()

        updateState { copy(
            projectUiVersion = appVersion.toString(),
            currentChannel = currentChannel,
        ) }
    }
}
