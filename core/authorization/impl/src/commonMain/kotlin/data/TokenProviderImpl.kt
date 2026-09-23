package data

import Authorization
import TokenProvider

class TokenProviderImpl(
    private val authorization: Authorization
) : TokenProvider {
    override fun provide(): String? {
        return try {
            val token = authorization.getToken()
            println("[DIAG] TokenProvider: token=present")
            token
        } catch (_: IllegalStateException) {
            println("[DIAG] TokenProvider: token=null (not authed)")
            null
        }
    }
}