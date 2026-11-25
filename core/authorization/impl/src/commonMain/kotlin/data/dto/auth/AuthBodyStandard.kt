package data.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthBodyStandard(
    @SerialName("client_name") val clientName: String,
    @SerialName("login") val email: String,
    @SerialName("password") val password: String,
)
