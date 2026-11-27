import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Навигатор корневого стека.
 *
 * Фичи внутри контейнера вкладок получают локальный навигатор,
 * который не умеет открывать корневые маршруты. Для перехода на корневой экран
 * (например, на экран авторизации после выхода) нужен именно этот навигатор.
 * */
val LocalRootNavigator = staticCompositionLocalOf<AppNavigator> {
    error("Корневой навигатор не предоставлен")
}
