actual fun createKtorClient(tokenProvider: Lazy<TokenProvider>): KtorClient =
    JvmKtorClient(tokenProvider)
