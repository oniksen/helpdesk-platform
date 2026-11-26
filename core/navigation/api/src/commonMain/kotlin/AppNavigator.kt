import androidx.navigation3.runtime.NavKey

interface AppNavigator {
    fun navigate(route: NavKey)
    fun popBackStack()
    /** Переход на маршрут с очисткой всего back-stack (без возврата на предыдущие экраны). */
    fun navigateAndClearBackStack(route: NavKey)
}