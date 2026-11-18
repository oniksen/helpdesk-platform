package state

import AppException

sealed interface CheckUpdateState {
    data object Idle : CheckUpdateState
    data object Loading : CheckUpdateState
    data object IsUpToDate : CheckUpdateState
    data object Available : CheckUpdateState
    data class Error(val error: AppException.UpdateError) : CheckUpdateState
}