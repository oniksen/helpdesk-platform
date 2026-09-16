package domain.model

data class TaskData(
    val building: Int?,
    val category: String,
    val commercial: Boolean,
    val contacts: List<String>,
    val dateCreate: String?,
    val description: String,
    val id: Int,
    val initiator: String,
    val reopenCount: Int,
    val serviceId: String,
    val status: String?,
    val statuses: Statuses,
    val title: String
)
