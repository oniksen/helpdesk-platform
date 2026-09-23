package presentation.viewmodel

import AppNavigator
import navigation.TasksPageRoute

internal actual fun platformFinishInstall(navigator: AppNavigator): FinishInstallResult {
    navigator.popBackStack()
    navigator.navigate(TasksPageRoute)
    return FinishInstallResult.Reloaded
}
