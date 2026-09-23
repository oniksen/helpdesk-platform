package data.storage

/**
 * Платформенное хранилище сериализованного состояния авторизации.
 * */
interface AuthStateStorage {
    /** Сохранить состояние авторизации в виде JSON-строки. */
    fun save(stateJson: String)

    /** Прочитать сохранённое состояние авторизации или вернуть null, если его нет. */
    fun load(): String?

    /** Удалить сохранённое состояние авторизации. */
    fun clear()
}

/** Создать платформенную реализацию хранилища состояния авторизации. */
expect fun createAuthStateStorage(): AuthStateStorage
