package domain

import domain.model.UpdateState
import domain.model.response.access.DevChannelAccess
import domain.model.response.action.ActionResponse
import domain.models.manifest.v2.UpdateManifest
import kotlinx.coroutines.flow.Flow

/**
 * Менеджер обновлений приложения.
 *
 * Предоставляет API для проверки наличия обновлений и выполнения полного цикла
 * установки: загрузка → верификация → распаковка → замена файлов → перезапуск.
 */
interface UpdateManager {
    /**
     * Поток состояний процесса обновления.
     * */
    val updateState: Flow<UpdateState>

    /**
     * Проверяет наличие обновлений для указанной системной версии.
     *
     * Манифест запрашивается при каждом вызове и не кэшируется.
     *
     * @param currentSystemVersion Системный номер версии в формате `X.Y.Z`.
     * @return [UpdateManifest] с информацией о последней доступной версии, или `null`,
     * если версия невалидна, обновление не требуется, канал или платформа
     * не поддерживаются либо манифест недоступен.
     */
    suspend fun loadManifest(currentSystemVersion: String): UpdateManifest?

    /**
     * Запускает процесс установки обновления.
     *
     * Скачанный архив проверяется по размеру и SHA-256 — оба значения обязательны.
     *
     * @param updateUrl URL для скачивания zip-архива с обновлением.
     * @param expectedHash Ожидаемый SHA-256 хеш архива из манифеста.
     * @param expectedSize Ожидаемый размер архива в байтах из манифеста.
     */
    fun performUpdate(
        updateUrl: String,
        expectedHash: String,
        expectedSize: Long,
    )

    /**
     * @see [domain.port.VersionActions.markAsStable]
     * */
    suspend fun markAsStable(): ActionResponse?

    /**
     * @see [domain.port.VersionActions.reject]
     * */
    suspend fun reject(reason: String): ActionResponse?

    /**
     * @see [domain.port.VersionActions.publish]
     * */
    suspend fun publish(): ActionResponse?

    /**
     * @see [domain.port.VersionActions.devChannelAccess]
     * */
    suspend fun devChannelAccess(): DevChannelAccess?
}
