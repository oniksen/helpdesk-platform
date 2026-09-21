package presentation.effect

sealed class ParkingScreenEffect {
    data class ShowSnackBar(val message: String) : ParkingScreenEffect()
}
