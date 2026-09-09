sealed interface MaxAuthorizationResult {
    data class Success(val initData: String) : MaxAuthorizationResult
    data class Error(val error: Throwable) : MaxAuthorizationResult
    data object Unavailable : MaxAuthorizationResult
}