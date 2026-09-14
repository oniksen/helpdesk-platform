package data.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    @SerialName("access_level") val accessLevelDto: String? = null,
    @SerialName("birth_day") val birthDayDto: String? = null,
    @SerialName("buildings") val buildingsDto: List<String?>? = null,
    @SerialName("director") val directorDto: Boolean? = null,
    @SerialName("email") val emailDto: String? = null,
    @SerialName("gender") val genderDto: String? = null,
    @SerialName("id") val idDto: String? = null,
    @SerialName("last_name") val lastNameDto: String? = null,
    @SerialName("name") val nameDto: String? = null,
    @SerialName("personal_phone") val personalPhoneDto: String? = null,
    @SerialName("second_name") val secondNameDto: String? = null,
    @SerialName("services") val servicesDto: List<Int?>? = null,
    @SerialName("work_city") val workCityDto: String? = null,
    @SerialName("work_company") val workCompanyDto: String? = null,
    @SerialName("work_department") val workDepartmentDto: String? = null,
    @SerialName("work_notes") val workNotesDto: String? = null,
    @SerialName("work_phone") val workPhoneDto: String? = null,
    @SerialName("work_position") val workPositionDto: String? = null,
    @SerialName("work_street") val workStreetDto: String? = null,
    @SerialName("user_groups") val userGroupsDto: List<String?>? = null,
)