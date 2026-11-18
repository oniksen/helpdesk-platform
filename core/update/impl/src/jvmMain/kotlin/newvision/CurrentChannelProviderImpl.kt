package newvision

import domain.models.channels.ChannelAccess

class CurrentChannelProviderImpl: CurrentChannelProvider {
    private var _currentChannel: ChannelAccess? = null

    override fun saveCurrentAccessState(state: ChannelAccess) {
        _currentChannel = state
    }

    override fun getCurrentAccessState(): ChannelAccess? {
        return _currentChannel
    }
}