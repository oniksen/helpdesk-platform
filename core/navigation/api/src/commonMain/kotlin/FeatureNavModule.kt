import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule

interface FeatureNavModule {
    /** Сериализатор маршрута для Web/iOS. */
    val serializerModule: SerializersModule

    /** Проверка: умеет ли этот модуль обрабатывать указанный маршрут. */
    fun canResolve(key: NavKey): Boolean

    /** Метод отрисовки экрана. */
    fun resolve(key: NavKey, navigator: AppNavigator): NavEntry<*>
}