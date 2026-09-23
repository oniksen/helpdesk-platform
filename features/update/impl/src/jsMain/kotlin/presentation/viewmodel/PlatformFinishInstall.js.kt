package presentation.viewmodel

import AppNavigator
import incrementInstallReloadAttempts
import kotlinx.browser.window
import markAppInstalled
import requestSwVersion
import resetInstallReloadAttempts

private const val MAX_INSTALL_RELOAD_ATTEMPTS = 3

internal actual fun platformFinishInstall(navigator: AppNavigator): FinishInstallResult {
    val controller = window.navigator.serviceWorker.controller
    println("[DIAG] finishInstall: controller=${controller?.state ?: "NO CONTROLLER"} script=${controller?.scriptURL}")

    val attempts = incrementInstallReloadAttempts()
    if (attempts > MAX_INSTALL_RELOAD_ATTEMPTS) {
        resetInstallReloadAttempts()
        println("[DIAG] finishInstall: reload aborted after $attempts attempts (SW did not hand off to full app)")
        return FinishInstallResult.Failed
    }

    requestSwVersion()
    println("[DIAG] finishInstall: markAppInstalled + reload (attempt $attempts)")
    markAppInstalled()
    window.location.reload()
    return FinishInstallResult.Reloaded
}
