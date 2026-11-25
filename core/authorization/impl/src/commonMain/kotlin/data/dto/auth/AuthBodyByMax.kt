package data.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthBodyByMax(
    @SerialName("init_data") val initData: String
)
