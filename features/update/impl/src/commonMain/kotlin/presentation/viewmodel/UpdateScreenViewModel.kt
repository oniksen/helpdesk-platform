package presentation.viewmodel

import AppNavigator
import AppUpdater
import DEFAULT_WEB_APP_URL
import UpdateScreenRoute
import presentation.intent.UpdateScreenIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import navigation.TasksPageRoute
import presentation.effect.UpdateScreenEffect
import presentation.state.UpdateState
import presentation.state.UpdateStatus
import kotlin.time.Duration.Companion.milliseconds

internal expect fun platformRegisterServiceWorker()

internal class UpdateScreenViewModel(
    private val navigator: AppNavigator,
    private val updater: AppUpdater,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val state: StateFlow<UpdateState>
        field = MutableStateFlow(UpdateState())
    val effect: SharedFlow<UpdateScreenEffect>
        field = MutableSharedFlow<UpdateScreenEffect>(
            replay = 1,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

    fun sendIntent(intent: UpdateScreenIntent) {
        when (intent) {
            UpdateScreenIntent.StartUpdate -> startUpdate()
        }
    }

    init {
        startUpdate()
    }

    private fun startUpdate() {
        updateState {
            copy(
                inProgress = true,
                status = UpdateStatus.Downloading,
                message = "Загрузка...",
            )
        }

        scope.launch(Dispatchers.Default) {
            try {
                val cached = updater.getCachedBuild(DEFAULT_WEB_APP_URL)
                if (cached != null) {
                    applyUpdate()
                    return@launch
                }

                updater.downloadAndUnpack(DEFAULT_WEB_APP_URL)

                updateState {
                    copy(
                        status = UpdateStatus.Unpacking,
                        message = "Распаковка...",
                    )
                }

                delay(300.milliseconds)
                applyUpdate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showError(e.message ?: "Ошибка обновления")
            }
        }
    }

    private suspend fun applyUpdate() {
        updateState {
            copy(
                status = UpdateStatus.Installing,
                message = "Установка...",
            )
        }

        platformRegisterServiceWorker()

        updateState {
            copy(
                inProgress = false,
                status = UpdateStatus.Success,
                message = "Успешная установка",
            )
        }

        delay(1_000.milliseconds)
        navigator.navigate(TasksPageRoute)
    }

    private fun showError(message: String) {
        effect.tryEmit(UpdateScreenEffect.ShowSnackBar(message))

        updateState {
            copy(
                inProgress = false,
                status = UpdateStatus.Error,
                message = "Ошибка обновления. Повторите снова",
            )
        }
    }

    private fun updateState(block: UpdateState.() -> UpdateState) {
        state.update { state -> block(state) }
    }
}
