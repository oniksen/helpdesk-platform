package presentation.viewmodel

/** Результат завершения установки: reload / отказ (после повторов). */
internal sealed interface FinishInstallResult {
    data object Reloaded : FinishInstallResult
    data object Failed : FinishInstallResult
}
