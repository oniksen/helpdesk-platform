package data

import Authorization
import TokenProvider

class TokenProviderImpl(
    private val authorization: Authorization
) : TokenProvider {
    override fun provide(): String? {
        return try {
            authorization.getToken()
        } catch (_: IllegalStateException) {
            null
        }
    }
}