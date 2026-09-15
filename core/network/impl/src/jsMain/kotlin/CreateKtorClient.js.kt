import io.ktor.client.engine.js.Js

actual fun createKtorClient(tokenProvider: Lazy<TokenProvider>): KtorClient {
    return KtorClientImpl(
        engine = Js,
        tokenProvider = tokenProvider,
    )
}