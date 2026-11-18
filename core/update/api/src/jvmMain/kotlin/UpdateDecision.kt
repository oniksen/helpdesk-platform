sealed class UpdateDecision {
    data object UpToDate : UpdateDecision()
    data class UpdateAvailable(val url: String) : UpdateDecision()
    data class ManifestError(val error: AppException) : UpdateDecision()
}
