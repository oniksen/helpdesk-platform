import io.ktor.client.engine.cio.CIO

actual fun createKtorClient(tokenProvider: Lazy<TokenProvider>): KtorClient {
    return KtorClientImpl(
        tokenProvider = tokenProvider,
        engine = CIO
    )
}
