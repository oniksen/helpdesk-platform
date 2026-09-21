package presentation.state

data class ParkingScreenState(
    val passNumber: String = "",
    val passError: String? = null,
    val showQrBtnEnabled: Boolean = true,
    val passInputState: PassInputState = PassInputState.Idle,
    val authSheetVisible: Boolean = false,
    val testAuthInfo: String? = null,
)