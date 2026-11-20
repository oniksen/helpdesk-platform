package presentation.effect

sealed class SettingsEffect {
    data class ShowSnackBar(val message: String): SettingsEffect()
}
