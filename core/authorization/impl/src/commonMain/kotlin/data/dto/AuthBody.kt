package data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthBody(
    @SerialName("init_data") val initData: String
)
