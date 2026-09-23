actual fun isAppInstalled(): Boolean = false

actual fun markAppInstalled() = Unit

actual fun isServiceWorkerControlled(): Boolean = false

actual fun incrementInstallReloadAttempts(): Int = 1

actual fun resetInstallReloadAttempts() = Unit
