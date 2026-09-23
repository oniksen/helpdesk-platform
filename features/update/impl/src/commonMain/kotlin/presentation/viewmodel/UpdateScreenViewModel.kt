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
import presentation.effect.UpdateScreenEffect
import presentation.state.UpdateState
import presentation.state.UpdateStatus
import shouldSkipUpdate
import kotlin.time.Duration.Companion.milliseconds

internal expect suspend fun platformRegisterServiceWorker(): Boolean

internal expect suspend fun platformPushBuild(url: String, files: Map<String, ByteArray>): Boolean

internal class UpdateScreenViewModel(
    private val navigator: AppNavigator,
    private val updater: AppUpdater,
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
        println("[DIAG] update: start url=$DEFAULT_WEB_APP_URL")

        if (shouldSkipUpdate()) {
            scope.launch { finishInstall() }
            return
        }

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
                println(
                    "[DIAG] update: cache hit=${cached != null} files=${cached?.size} " +
                        "hasIndexHtml=${cached?.containsKey("index.html")} " +
                        "keys=${cached?.keys?.take(6)?.joinToString(",")}"
                )
                cached?.get("index.html")?.let { html ->
                    val snippet = html.decodeToString(0, minOf(400, html.size))
                    val refsWebApp = snippet.contains("webApp.js")
                    val refsWebShell = snippet.contains("webShell.js")
                    println(
                        "[DIAG] cached index.html: refsWebApp=$refsWebApp refsWebShell=$refsWebShell " +
                            "snippet=$snippet"
                    )
                }
                buildFiles = cached ?: downloadBuild()
                applyUpdate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showError(e.message ?: "Ошибка обновления")
            }
        }
    }

    private suspend fun downloadBuild(): Map<String, ByteArray> {
        val files = updater.downloadAndUnpack(DEFAULT_WEB_APP_URL)
        println("[DIAG] update: downloaded ok files=${files.size}")

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
        println("[DIAG] update: applyUpdate started")
        updateState {
            copy(
                status = UpdateStatus.Installing,
                message = "Установка...",
            )
        }

        val swControlled = platformRegisterServiceWorker()
        println("[DIAG] update: registerServiceWorker called controlled=$swControlled")
        if (!swControlled) {
            showError("Service Worker недоступен: установка не может завершиться")
            return
        }

        val files = buildFiles
        if (files != null) {
            println("[DIAG] update: pushing build to SW (${files.size} files)")
            val pushed = platformPushBuild(DEFAULT_WEB_APP_URL, files)
            println("[DIAG] update: build pushed via SW=$pushed")
            if (!pushed) {
                showError("Не удалось передать сборку в Service Worker")
                return
            }
        } else {
            println("[DIAG] update: no build files, skipping SW push")
        }

        updateState {
            copy(
                inProgress = false,
                status = UpdateStatus.Success,
                message = "Успешная установка",
            )
        }

        delay(1_000.milliseconds)
        println("[DIAG] update: calling finishInstall")
        finishInstall()
    }

    private suspend fun finishInstall() {
        val result = platformFinishInstall(navigator)
        if (result == FinishInstallResult.Failed) {
            showError("Установка завершена, но приложение не переключилось на новую сборку. Обновите страницу вручную.")
        }
    }

    private fun showError(message: String) {
        println("[DIAG] update: error $message")
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
