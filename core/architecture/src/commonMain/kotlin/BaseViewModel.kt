import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<State, Effect>(
    private val initialState: State,
) {
    protected val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val uiState: StateFlow<State>
        field = MutableStateFlow(initialState)
    val effect: SharedFlow<Effect>
        field = MutableSharedFlow<Effect>(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    protected fun updateState(block: State.() -> State) {
        uiState.update { block(uiState.value) }
    }

    protected fun updateEffect(block: () -> Effect) {
        effect.tryEmit(block())
    }

    protected fun<T> NetworkResult<out T>.getResultOrSendEffect(
        callableError: (AppException) -> Unit,
    ): T? {
        return when (this) {
            is NetworkResult.Success -> this.data
            is NetworkResult.Error -> {
                callableError(this.exception)
                null
            }
        }
    }
}