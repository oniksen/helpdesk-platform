package domain.model.response.access

data class DevChannelAccess(
    val schemaVersion: Int,
    val users: List<User>
)
