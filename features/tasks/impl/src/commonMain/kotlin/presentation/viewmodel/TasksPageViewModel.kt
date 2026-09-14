package presentation.viewmodel

import AppNavigator
import navigation.ParkingScreenRoute
import presentation.intent.TasksPageIntent

internal class TasksPageViewModel(
    private val navigator: AppNavigator
) {
    fun sendIntent(intent: TasksPageIntent) {
        when (intent) {
            TasksPageIntent.OpenParkingPage -> openParkingPage()
        }
    }

    private fun openParkingPage() {
        navigator.navigate(ParkingScreenRoute)
    }
}