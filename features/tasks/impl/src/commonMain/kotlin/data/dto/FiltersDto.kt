package data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FiltersDto(
    @SerialName("buildings") val buildings: List<Int>,
)

