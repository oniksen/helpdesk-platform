package presentation.viewmodel

import AppNavigator
import incrementInstallReloadAttempts
import kotlinx.browser.window
import markAppInstalled
import resetInstallReloadAttempts

private const val MAX_INSTALL_RELOAD_ATTEMPTS = 3

internal actual fun platformFinishInstall(navigator: AppNavigator): FinishInstallResult {
    val attempts = incrementInstallReloadAttempts()
    if (attempts > MAX_INSTALL_RELOAD_ATTEMPTS) {
        resetInstallReloadAttempts()
        return FinishInstallResult.Failed
    }

    markAppInstalled()
    window.location.reload()
    return FinishInstallResult.Reloaded
}
