import domain.UpdateChannel

actual class CurrentChannelProviderImpl : CurrentChannelProvider {
    /**
     * JVM-приложение публикуется только в prod-ветку: dev-каналы
     * относятся к web-сборкам и пользовательскими настройками не выбираются.
     */
    actual override fun provide(): String? = UpdateChannel.PROD
}
