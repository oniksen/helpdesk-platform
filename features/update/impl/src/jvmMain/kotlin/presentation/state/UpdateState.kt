package presentation.state

data class UpdateState(
    val status: UpdateStatus = UpdateStatus.Idle,
    val message: String? = null,
    val inProgress: Boolean = false,
)

enum class UpdateStatus {
    Idle,
    Downloading,
    Unpacking,
    Installing,
    Success,
    Error,
}
