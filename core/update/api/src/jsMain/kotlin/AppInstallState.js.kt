import kotlinx.browser.window

private const val INSTALLED_FLAG_KEY = "helpdesk.installed"
private const val RELOAD_ATTEMPTS_KEY = "helpdesk.installReloadAttempts"

actual fun isAppInstalled(): Boolean =
    window.localStorage.getItem(INSTALLED_FLAG_KEY) != null

actual fun markAppInstalled() {
    window.localStorage.setItem(INSTALLED_FLAG_KEY, "1")
}

actual fun isServiceWorkerControlled(): Boolean =
    window.navigator.serviceWorker.controller != null

actual fun incrementInstallReloadAttempts(): Int {
    val current = window.localStorage.getItem(RELOAD_ATTEMPTS_KEY)?.toIntOrNull() ?: 0
    val next = current + 1
    window.localStorage.setItem(RELOAD_ATTEMPTS_KEY, next.toString())
    return next
}

actual fun resetInstallReloadAttempts() {
    window.localStorage.removeItem(RELOAD_ATTEMPTS_KEY)
}
