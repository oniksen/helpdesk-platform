package data.dto.auth.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    @SerialName("bearer_token") val bearerTokenDto: String? = null,
    @SerialName("cookies") val cookiesDto: String? = null,
    @SerialName("user_id") val userIdDto: Int? = null,
    @SerialName("email") val email: String? = null,
)