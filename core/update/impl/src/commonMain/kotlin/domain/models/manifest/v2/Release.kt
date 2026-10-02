package domain.models.manifest.v2

/**
 * Набор таргетов одной ветки обновлений: prod, canary или rc.
 *
 * Таргет [null], если под него ещё не собрана сборка.
 * */
data class Release(
    val macos: TargetData?,
    val web: TargetData?,
    val windows: TargetData?,
)
