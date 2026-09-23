package data.storage

import kotlinx.browser.window

private const val AUTH_STATE_KEY = "helpdesk.auth.v1"

actual fun createAuthStateStorage(): AuthStateStorage = JsAuthStateStorage

private object JsAuthStateStorage : AuthStateStorage {
    override fun save(stateJson: String) {
        window.localStorage.setItem(AUTH_STATE_KEY, stateJson)
    }

    override fun load(): String? =
        window.localStorage.getItem(AUTH_STATE_KEY)

    override fun clear() {
        window.localStorage.removeItem(AUTH_STATE_KEY)
    }
}
