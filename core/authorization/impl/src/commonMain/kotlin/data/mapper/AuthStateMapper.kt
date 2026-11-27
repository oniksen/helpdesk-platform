package data.mapper

import auth.AuthData
import data.dto.authstate.AuthStateDto

internal fun AuthStateDto.toAuthData(): AuthData? {
    val token = this.token ?: return null
    return AuthData(
        token = token,
        cookies = null,
    )
}
