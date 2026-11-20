package data.impl

import domain.model.response.access.DevChannelAccess
import domain.model.response.action.ActionResponse
import domain.port.VersionActions

/**
 * Управление версиями программы.
 *
 * Контракт сохранён для будущих релизных сценариев (стабилизация, отклонение,
 * публикация в prod), но серверные вызовы пока не реализованы.
 */
class VersionActionsImpl : VersionActions {

    override suspend fun markAsStable(): ActionResponse? = TODO("Not implemented yet")

    override suspend fun reject(reason: String): ActionResponse? = TODO("Not implemented yet")

    override suspend fun publish(): ActionResponse? = TODO("Not implemented yet")

    override suspend fun devChannelAccess(): DevChannelAccess? = TODO("Not implemented yet")
}
