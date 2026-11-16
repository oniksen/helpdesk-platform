package data.storage

actual fun createAuthStateStorage(): AuthStateStorage = JvmAuthStateStorage

private object JvmAuthStateStorage : AuthStateStorage {
    override fun save(stateJson: String) {
        TODO("Not yet implemented")
    }

    override fun load(): String? {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }
}