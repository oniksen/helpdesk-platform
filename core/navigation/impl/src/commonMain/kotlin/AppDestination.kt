import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CarRental
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import navigation.TasksPageRoute
import navigation.ParkingScreenRoute

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val route: NavKey,
) {
    PARKING(
        label = "Парковка",
        icon = Icons.Outlined.CarRental,
        route = ParkingScreenRoute,
    ),
    TASKS(
        label = "Задачи",
        icon = Icons.Outlined.TaskAlt,
        route = TasksPageRoute,
    )
}