import androidx.navigation3.runtime.NavKey

interface AppNavigator {
    fun navigate(route: NavKey)
    fun popBackStack()
}