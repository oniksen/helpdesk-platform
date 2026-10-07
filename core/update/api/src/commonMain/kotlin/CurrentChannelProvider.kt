interface CurrentChannelProvider {
    /** @return код канала (напр. "dev.canary") или null, если ещё не определён */
    fun provide(): String?
}
