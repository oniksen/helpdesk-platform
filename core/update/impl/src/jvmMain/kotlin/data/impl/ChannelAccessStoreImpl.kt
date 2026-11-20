package data.impl

import domain.models.channels.ChannelAccess
import domain.port.ChannelAccessStore

/**
 * In-memory хранилище последнего известного доступа к каналам.
 *
 * Значение пишется и читается из корутин менеджера; [Volatile] оставлен
 * на случай будущего чтения из другого потока (например, из UI).
 */
class ChannelAccessStoreImpl : ChannelAccessStore {
    @Volatile
    private var access: ChannelAccess? = null

    override fun save(access: ChannelAccess) {
        this.access = access
    }

    override fun current(): ChannelAccess? = access
}
