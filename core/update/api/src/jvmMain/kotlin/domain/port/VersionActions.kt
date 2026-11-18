package domain.port

import domain.model.response.access.DevChannelAccess
import domain.model.response.action.ActionResponse

/**
 * Порт управления версиями программы.
 * */
interface VersionActions {
    /**
     * Отметить текущую версию обновления в DEV канале CURRENT треке, как STABLE.
     * */
    suspend fun markAsStable(): ActionResponse?

    /**
     * Отклонить текущую версию обновления в DEV канале и CURRENT треке.
     *
     * @param reason Обязательное сообщение с описанием причины отклонения версии.
     * */
    suspend fun reject(reason: String): ActionResponse?

    /**
     * Разместить версию из DEV Stable канала в PROD канале.
     * */
    suspend fun publish(): ActionResponse?

    /**
     * Получить данные по пользователям, имеющим доступ к dev-каналу.
     * */
    suspend fun devChannelAccess(): DevChannelAccess?
}
