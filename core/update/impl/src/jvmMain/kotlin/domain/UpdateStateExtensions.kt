package domain

import domain.model.CurrentStageState
import domain.model.UpdateState

/** Признак того, что последнее состояние этапа — ошибка. */
internal fun UpdateState?.hasStageError(): Boolean = when (this) {
    is UpdateState.Loading -> stage is CurrentStageState.Error
    is UpdateState.Verification -> stage is CurrentStageState.Error
    is UpdateState.IntegrityChecking -> stage is CurrentStageState.Error
    is UpdateState.Unpacking -> stage is CurrentStageState.Error
    is UpdateState.Installation -> stage is CurrentStageState.Error
    is UpdateState.CleaningTemporaryFiles -> stage is CurrentStageState.Error
    null, UpdateState.Idle, is UpdateState.Restarting -> false
}
