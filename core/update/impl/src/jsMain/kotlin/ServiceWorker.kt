import kotlinx.browser.window

private const val SW_PATH = "/update-sw.js"

fun registerServiceWorker() {
    window.navigator.serviceWorker.register(SW_PATH).then(
        onFulfilled = { registration ->
            console.log("[SW] Registered scope: ${registration.scope}")
        },
        onRejected = { error ->
            console.error("[SW] Registration failed: $error")
        }
    )
}

fun unregisterServiceWorker() {
    window.navigator.serviceWorker.getRegistrations().then { registrations ->
        registrations.forEach { it.unregister() }
        console.log("[SW] Unregistered all service workers")
    }
}
