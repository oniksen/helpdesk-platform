package data.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthResponseDto(
    val cookies: String,
    @SerialName("bearer_token") val bearerToken: String,
)
