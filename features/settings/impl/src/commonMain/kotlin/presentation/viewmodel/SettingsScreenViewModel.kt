package presentation.viewmodel

import AppVersionProvider
import BaseViewModel
import presentation.effect.SettingsEffect
import presentation.state.SettingsUiState

class SettingsScreenViewModel(
    appVersionProvider: AppVersionProvider,
): BaseViewModel<SettingsUiState, SettingsEffect>(SettingsUiState()) {
    init {
        appVersionProvider.provide().let { version ->
            updateState { copy(
                projectUiVersion = version.toString()
            ) }
        }
    }
}
