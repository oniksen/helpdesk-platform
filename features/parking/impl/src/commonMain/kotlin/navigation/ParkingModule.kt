package navigation

import AppNavigator
import FeatureNavModule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.koinInject
import presentation.screen.ParkingScreen
import presentation.viewmodel.ParkingScreenViewModel

class ParkingModule : FeatureNavModule {
    override val serializerModule = SerializersModule {
        polymorphic(NavKey::class) { subclass(ParkingScreenRoute::class, ParkingScreenRoute.serializer()) }
    }

    override fun canResolve(key: NavKey): Boolean = key is ParkingScreenRoute

    override fun resolve(
        key: NavKey,
        navigator: AppNavigator
    ): NavEntry<out NavKey> = NavEntry(key = key as ParkingScreenRoute) {
        val parkingScreenViewModel = ParkingScreenViewModel(
            qrCodeScanner = koinInject(),
        )

        ParkingScreen(
            viewModel = parkingScreenViewModel,
        )
    }
}