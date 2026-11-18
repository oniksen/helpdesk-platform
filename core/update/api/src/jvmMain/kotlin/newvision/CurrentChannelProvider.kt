package newvision

import domain.models.channels.ChannelAccess

/**
 * Объект для управления текущим разрешённым каналом.
 * */
interface CurrentChannelProvider {
    fun saveCurrentAccessState(state: ChannelAccess)
    fun getCurrentAccessState(): ChannelAccess?
}