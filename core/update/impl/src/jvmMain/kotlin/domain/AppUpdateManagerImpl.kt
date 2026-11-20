package domain

import AppException
import AppVersionProvider
import domain.model.AvailableUpdate
import domain.model.ChannelAccessSnapshot
import domain.model.ChannelAccessSource
import domain.model.CheckUpdateState
import domain.model.CurrentStageState
import domain.model.UnavailableReason
import domain.model.UpdateChannel
import domain.model.UpdateState
import domain.model.targetFor
import domain.models.channels.ChannelAccess
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest
import domain.port.ChannelAccessStore
import domain.port.UpdateDownloader
import domain.port.ZipExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import normalizeFromTechnical
import platform.AppInstallDirResolver
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Реализация [AppUpdateManager] для JVM-приложения.
 *
 * Проверка обновления и установка разделены:
 * - [checkUpdate] читает каналы и манифест, выбирает канал и таргет,
 *   публикует терминальное состояние в [checkUpdateState];
 * - [startUpdate] запускает проверенный пайплайн [UpdateInstaller] в
 *   собственном scope и публикует прогресс в [updateState].
 */
class AppUpdateManagerImpl(
    private val updateDownloader: UpdateDownloader,
    private val zipExtractor: ZipExtractor,
    private val appRestarter: AppRestarter,
    private val appInstallDirResolver: AppInstallDirResolver,
    private val channelAccessStore: ChannelAccessStore,
    private val versionProvider: AppVersionProvider,
    private val platform: UpdatePlatform? = currentUpdatePlatform(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val downloadFileFactory: () -> File = { createDownloadFile(platform) },
) : AppUpdateManager {

    override val checkUpdateState: StateFlow<CheckUpdateState>
        field = MutableStateFlow<CheckUpdateState>(CheckUpdateState.Idle)

    override val updateState: StateFlow<UpdateState>
        field = MutableStateFlow<UpdateState>(UpdateState.Idle)

    override val channelAccessState: StateFlow<ChannelAccessSnapshot>
        field = MutableStateFlow(ChannelAccessSnapshot())

    private val checkMutex = Mutex()
    private val isUpdating = AtomicBoolean(false)

    override suspend fun checkUpdate(email: String?) {
        checkMutex.withLock {
            runCheck(email)
        }
    }

    override fun startUpdate() {
        val available = checkUpdateState.value as? CheckUpdateState.Available ?: return
        val currentPlatform = platform ?: return
        if (!isUpdating.compareAndSet(false, true)) return

        scope.launch {
            try {
                UpdateInstaller(
                    updateDownloader = updateDownloader,
                    zipExtractor = zipExtractor,
                    appRestarter = appRestarter,
                    appInstallDirResolver = appInstallDirResolver,
                    platform = currentPlatform,
                    downloadFileFactory = downloadFileFactory,
                ).install(available.update) { state ->
                    updateState.value = state
                }
            } catch (e: CancellationException) {
                throw e
            } catch (error: Throwable) {
                if (!updateState.value.hasStageError()) {
                    updateState.value = UpdateState.Loading(
                        CurrentStageState.Error(error.localizedMessage),
                    )
                }
                println("[AppUpdateManager] Неожиданная ошибка установки: ${error.message}")
            } finally {
                isUpdating.set(false)
            }
        }
    }

    /**
     * Проверка обновления. Терминальное состояние публикуется до возврата
     * из [checkUpdate], поэтому вызывающий может читать [checkUpdateState.value].
     */
    private suspend fun runCheck(email: String?) {
        checkUpdateState.value = CheckUpdateState.Loading

        try {
            val state = evaluate(email)
            checkUpdateState.value = state
            if (state is CheckUpdateState.Error) {
                println("[AppUpdateManager] Проверка обновлений не выполнена: ${state.error.message}")
            }
        } catch (e: CancellationException) {
            checkUpdateState.value = CheckUpdateState.Idle
            throw e
        } catch (error: Throwable) {
            checkUpdateState.value = CheckUpdateState.Error(
                AppException.UpdateError.CheckFailed(cause = error),
            )
            println("[AppUpdateManager] Проверка обновлений не выполнена: ${error.message}")
        }
    }

    private suspend fun evaluate(email: String?): CheckUpdateState {
        val currentPlatform = platform ?: run {
            channelAccessState.value = ChannelAccessSnapshot()
            return CheckUpdateState.Unavailable(UnavailableReason.UNSUPPORTED_PLATFORM)
        }

        val normalizedEmail = email?.trim()?.takeIf { it.isNotEmpty() }
        val (access, manifestResult) = fetchInputs(normalizedEmail)

        if (access != null) {
            channelAccessStore.save(access)
        }

        val snapshot = resolveSnapshot(access, normalizedEmail)
        channelAccessState.value = snapshot

        val manifest = manifestResult.getOrNull()
            ?: return CheckUpdateState.Error(
                AppException.UpdateError.CheckFailed(
                    channel = snapshot.current.alias,
                    cause = manifestResult.exceptionOrNull(),
                ),
            )

        val target = manifest.targetFor(snapshot.current, currentPlatform)
            ?: return CheckUpdateState.Unavailable(UnavailableReason.NO_PUBLISHED_BUILD)

        return evaluateTarget(target, snapshot)
    }

    private fun evaluateTarget(
        target: TargetData,
        snapshot: ChannelAccessSnapshot,
    ): CheckUpdateState {
        val availableVersion = try {
            target.version.normalizeFromTechnical()
        } catch (error: Exception) {
            return CheckUpdateState.Error(
                AppException.UpdateError.CheckFailed(
                    channel = snapshot.current.alias,
                    message = "Не удалось разобрать версию обновления",
                    cause = error,
                ),
            )
        }

        val installedVersion = try {
            versionProvider.provide()
        } catch (error: Exception) {
            return CheckUpdateState.Error(
                AppException.UpdateError.CheckFailed(
                    channel = snapshot.current.alias,
                    message = "Не удалось определить текущую версию",
                    cause = error,
                ),
            )
        }

        return if (availableVersion > installedVersion) {
            CheckUpdateState.Available(
                AvailableUpdate(
                    version = availableVersion,
                    channel = snapshot.current,
                    url = target.link,
                    hash = target.hash,
                    size = target.size,
                    patchNote = target.patchNote,
                ),
            )
        } else {
            CheckUpdateState.IsUpToDate
        }
    }

    /**
     * Параллельно читает файл каналов (если известен email) и манифест.
     *
     * Ошибка каналов не прерывает проверку: возвращается `null`, и канал
     * выбирается с fallback на prod. Ошибка манифеста сохраняется в [Result],
     * чтобы попасть в состояние с причиной.
     */
    private suspend fun fetchInputs(
        email: String?,
    ): Pair<ChannelAccess?, Result<UpdateManifest>> = coroutineScope {
        val channelsDeferred = async {
            if (email == null) {
                null
            } else {
                try {
                    updateDownloader.downloadChannelsManifest().accessFor(email)
                } catch (e: CancellationException) {
                    throw e
                } catch (error: Exception) {
                    println("[AppUpdateManager] Файл каналов недоступен: ${error.message}")
                    null
                }
            }
        }

        val manifestDeferred = async {
            try {
                Result.success(updateDownloader.downloadUpdateManifest())
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

        channelsDeferred.await() to manifestDeferred.await()
    }

    /**
     * Снимок каналов для UI.
     *
     * При ошибке channels.json эффективный канал — prod, но последние
     * известные allowed/selected остаются для отображения.
     */
    private fun resolveSnapshot(
        access: ChannelAccess?,
        email: String?,
    ): ChannelAccessSnapshot = when {
        access != null -> snapshotFrom(access, ChannelAccessSource.SERVER)

        email == null -> ChannelAccessSnapshot()

        else -> snapshotFrom(channelAccessStore.current(), ChannelAccessSource.FALLBACK)
            .copy(current = UpdateChannel.PROD)
    }

    private fun snapshotFrom(
        access: ChannelAccess?,
        source: ChannelAccessSource,
    ): ChannelAccessSnapshot {
        val allowed = access?.allowed.orEmpty()
            .mapNotNull(UpdateChannel::fromAlias)
            .distinct()
        val selected = UpdateChannel.fromAlias(access?.selected)
        val current = when {
            selected != null && (allowed.isEmpty() || selected in allowed) -> selected
            allowed.isNotEmpty() -> allowed.first()
            else -> UpdateChannel.PROD
        }

        return ChannelAccessSnapshot(
            allowed = allowed,
            selected = selected,
            current = current,
            source = source,
        )
    }
}
