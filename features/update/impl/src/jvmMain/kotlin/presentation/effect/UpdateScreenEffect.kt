package presentation.effect

sealed class UpdateScreenEffect {
    data class ShowSnackBar(val message: String) : UpdateScreenEffect()
}
