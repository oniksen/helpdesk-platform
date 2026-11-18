package domain

import domain.models.channels.ChannelAccess
import io.ktor.util.Platform
import kotlinx.coroutines.flow.Flow
import newvision.UpdateStateNewVision
import state.CheckUpdateState

/**
 * Центральный объект для управления состоянием обновлением в приложении.
 * */
interface AppUpdateManager {
    val checkUpdateState: Flow<CheckUpdateState>
    /**
     * Поток состояний процесса обновления.
     * */
    val updateState: Flow<UpdateStateNewVision>

    fun currentChannelAccess(): ChannelAccess

    /**
     * Проверка состояния обновления.
     * @param email Email пользователя для получения его данных о канале обновлений.
     **/
    suspend fun checkUpdate(email: String, currentPlatform: String)

    /**
     * Запуск процесса обновления.
     * */
    suspend fun startUpdate()
}