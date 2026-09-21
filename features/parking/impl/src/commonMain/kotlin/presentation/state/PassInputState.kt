package presentation.state

sealed class PassInputState {
    data object Idle : PassInputState()
    data object Checking : PassInputState()
    data object Success : PassInputState()
}