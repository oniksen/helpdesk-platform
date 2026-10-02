package presentation.viewmodel

import AppNavigator
import AppUpdater

import DEFAULT_WEB_APP_URL
import UpdateDecision
import UpdateScreenRoute
import presentation.intent.UpdateScreenIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import presentation.effect.UpdateScreenEffect
import presentation.state.UpdateState
import presentation.state.UpdateStatus
import shouldSkipUpdate
import kotlin.time.Duration.Companion.milliseconds

internal expect suspend fun platformRegisterServiceWorker(): Boolean

internal expect suspend fun platformPushBuild(url: String, files: Map<String, ByteArray>): Boolean

/**
 * Сообщение о сбое проверки обновлений, когда исключение не описало причину.
 * */
private const val MANIFEST_ERROR_MESSAGE = "Не удалось проверить обновление"

internal class UpdateScreenViewModel(
    private val navigator: AppNavigator,
    private val updater: AppUpdater,
    private val isUpdateSkippable: () -> Boolean = { shouldSkipUpdate() },
    private val registerServiceWorker: suspend () -> Boolean = { platformRegisterServiceWorker() },
    private val pushBuild: suspend (url: String, files: Map<String, ByteArray>) -> Boolean =
        { url, files -> platformPushBuild(url, files) },
    private val finishInstall: suspend () -> FinishInstallResult = { platformFinishInstall(navigator) },
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var buildFiles: Map<String, ByteArray>? = null

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
        scope.launch {
            val updateDecision = updater.checkForUpdate()

            if (isUpdateSkippable()) {
                // Сборку удерживает Service Worker, поэтому ставить нечего. Если
                // проверка обновлений не удалась, сообщаем об этом и оставляем
                // пользователю возможность повторить, а не молча уходим с экрана.
                if (updateDecision is UpdateDecision.ManifestError) {
                    showError(updateDecision.error.message ?: MANIFEST_ERROR_MESSAGE)
                    return@launch
                }

                if (updateDecision !is UpdateDecision.UpdateAvailable) {
                    scope.launch { completeInstall() }
                    return@launch
                }
            }

            updateState {
                copy(
                    inProgress = true,
                    status = UpdateStatus.Downloading,
                    message = "Загрузка...",
                )
            }

            scope.launch(backgroundDispatcher) {
                try {
                    val hasNewVersion = updateDecision is UpdateDecision.UpdateAvailable
                    val cached = if (hasNewVersion) null else updater.getCachedBuild(DEFAULT_WEB_APP_URL)
                    // При найденном обновлении кэш игнорируется: иначе в Service Worker
                    // уедет уже знакомая сборка и перезагрузка вернёт прежнюю версию.
                    buildFiles = cached ?: downloadBuild(forceRefresh = hasNewVersion)
                    applyUpdate()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    showError(e.message ?: "Ошибка обновления")
                }
            }
        }
    }

    private suspend fun downloadBuild(forceRefresh: Boolean): Map<String, ByteArray> {
        val files = updater.downloadAndUnpack(DEFAULT_WEB_APP_URL, forceRefresh)

        updateState {
            copy(
                status = UpdateStatus.Unpacking,
                message = "Распаковка...",
            )
        }

        delay(300.milliseconds)
        return files
    }

    private suspend fun applyUpdate() {
        updateState {
            copy(
                status = UpdateStatus.Installing,
                message = "Установка...",
            )
        }

        val swControlled = registerServiceWorker()
        if (!swControlled) {
            showError("Service Worker недоступен: установка не может завершиться")
            return
        }

        val files = buildFiles
        if (files != null) {
            val pushed = pushBuild(DEFAULT_WEB_APP_URL, files)
            if (!pushed) {
                showError("Не удалось передать сборку в Service Worker")
                return
            }
        }

        updateState {
            copy(
                inProgress = false,
                status = UpdateStatus.Success,
                message = "Успешная установка",
            )
        }

        delay(1_000.milliseconds)
        completeInstall()
    }

    private suspend fun completeInstall() {
        val result = finishInstall()
        if (result == FinishInstallResult.Failed) {
            showError("Установка завершена, но приложение не переключилось на новую сборку. Обновите страницу вручную.")
        }
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
