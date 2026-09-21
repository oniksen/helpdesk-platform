package data.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginationDto(
    @SerialName("has_next") val hasNextDto: Boolean? = null,
    @SerialName("has_previous") val hasPreviousDto: Boolean? = null,
    @SerialName("page") val pageDto: Int? = null,
    @SerialName("page_size") val pageSizeDto: Int? = null,
    @SerialName("total_items") val totalItemsDto: Int? = null,
    @SerialName("total_pages") val totalPagesDto: Int? = null
)