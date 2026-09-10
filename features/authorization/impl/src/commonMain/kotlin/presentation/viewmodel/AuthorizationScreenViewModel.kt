package presentation.viewmodel

import AppNavigator
import navigation.HomePageRoute
import presentation.intent.AuthorizationScreenIntent

internal class AuthorizationScreenViewModel(
    private val navigator: AppNavigator,
) {
    fun sendIntent(intent: AuthorizationScreenIntent) {
        when(intent) {
            AuthorizationScreenIntent.OpenHomeScreen -> openHomeScreen()
        }
    }

    private fun openHomeScreen() {
        navigator.navigate(HomePageRoute)
    }
}