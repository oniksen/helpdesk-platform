package domain.intent

sealed class ParkingScreenIntent {
    data object OpenScanner : ParkingScreenIntent()
    data object ResetScannerResult : ParkingScreenIntent()
    data class OnNumberChanged(val number: String) : ParkingScreenIntent()
    data object CheckPass : ParkingScreenIntent()
    data object GetPassDetails : ParkingScreenIntent()
}