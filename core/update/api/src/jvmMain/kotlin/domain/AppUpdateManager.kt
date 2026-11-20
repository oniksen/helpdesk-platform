package domain

import domain.model.ChannelAccessSnapshot
import domain.model.CheckUpdateState
import domain.model.UpdateState
import kotlinx.coroutines.flow.StateFlow

/**
 * Центральный вход в систему обновлений JVM-приложения.
 *
 * Два сценария: проверка обновления и запуск установки.
 * Состояния отдаются как read-only [StateFlow]: у них всегда есть текущее
 * значение, и UI не зависит от момента подписки.
 */
interface AppUpdateManager {
    /** Состояние проверки обновления. */
    val checkUpdateState: StateFlow<CheckUpdateState>

    /** Состояние процесса установки. */
    val updateState: StateFlow<UpdateState>

    /**
     * Последний известный доступ к каналам: allowed, selected и эффективный current.
     * Нужен будущему UI переключения каналов; не зависит от результата проверки.
     */
    val channelAccessState: StateFlow<ChannelAccessSnapshot>

    /**
     * Проверяет наличие обновления для текущей версии приложения.
     *
     * Канал: channels.json по email → selected / первый allowed → PROD.
     * Если email == null, channels.json не запрашивается, используется PROD.
     *
     * Результат не возвращается: терминальное состояние публикуется
     * в [checkUpdateState] до завершения функции. После вызова вызывающий
     * читает [checkUpdateState.value] (или подписывается на flow).
     *
     * @param email Email пользователя для получения доступа к каналам;
     * `null`, если авторизация ещё не выполнена.
     */
    suspend fun checkUpdate(email: String? = null)

    /**
     * Запускает установку найденного обновления.
     *
     * Fire-and-forget: выполняется в собственном scope менеджера.
     * Если обновление не найдено или установка уже идёт — ничего не делает.
     */
    fun startUpdate()
}
