package data

import data.dto.authstate.AuthStateDto
import data.storage.AuthStateStorage
import kotlinx.serialization.json.Json

/**
 * Удаляет сохранённую сессию при выходе из приложения,
 * если пользователь не включил автоматический вход.
 * */
internal class AuthStateCleaner(
    private val storage: AuthStateStorage,
) {
    fun clearIfAutoLoginDisabled() {
        val stateJson = storage.load() ?: return
        val autoLogin = runCatching { stateJsonFormat.decodeFromString<AuthStateDto>(stateJson) }
            .getOrNull()?.autoLogin ?: false

        if (!autoLogin) {
            storage.clear()
        }
    }

    private companion object {
        val stateJsonFormat = Json { ignoreUnknownKeys = true }
    }
}
