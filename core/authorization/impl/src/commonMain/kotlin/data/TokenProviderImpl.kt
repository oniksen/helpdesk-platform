package data

import Authorization
import TokenProvider

class TokenProviderImpl(
    private val authorization: Authorization
) : TokenProvider {
    override fun provide(): String? {
        return try {
            val token =  authorization.getToken()
            println(token)
            token
        } catch (_: IllegalStateException) {
            null
        }
    }
}