import web.storage.localStorage

actual class CurrentChannelProviderImpl : CurrentChannelProvider {
    actual override fun provide(): String? {
        return localStorage.getItem(ChannelStorage.LAST_CHANNEL_KEY)
    }
}
