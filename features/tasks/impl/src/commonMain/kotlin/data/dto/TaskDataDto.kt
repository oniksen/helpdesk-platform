package data.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskDataDto(
    @SerialName("building") val buildingDto: Int? = null,
    @SerialName("category") val categoryDto: String? = null,
    @SerialName("commercial") val commercialDto: Boolean? = null,
    @SerialName("contacts") val contactsDto: List<String?>? = null,
    @SerialName("date_create") val dateCreateDto: String? = null,
    @SerialName("description") val descriptionDto: String? = null,
    @SerialName("id") val idDto: String? = null,
    @SerialName("initiator") val initiatorDto: String? = null,
    @SerialName("reopen_count") val reopenCountDto: Int? = null,
    @SerialName("service_id") val serviceIdDto: String? = null,
    @SerialName("status") val statusDto: String? = null,
    @SerialName("statuses") val statusesDto: StatusesDto? = null,
    @SerialName("title") val titleDto: String? = null
)