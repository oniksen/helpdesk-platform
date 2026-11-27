package data.repository

import DownloadStatus
import KtorClient
import auth.AuthData
import data.storage.AuthStateStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import user.AccessLevel
import user.UserData
import user.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthorizationImplTest {

    private class MemoryStorage : AuthStateStorage {
        var saved: String? = null

        override fun save(stateJson: String) {
            saved = stateJson
        }

        override fun load(): String? = saved

        override fun clear() {
            saved = null
        }
    }

    private class FakeKtorClient(engine: MockEngine) : KtorClient {
        var tokenProvider: () -> String? = { null }

        val client = HttpClient(engine) {
            install(Auth) {
                bearer {
                    loadTokens {
                        tokenProvider()?.let { BearerTokens(accessToken = it, refreshToken = null) }
                    }
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        override fun baseInstance(): HttpClient = client

        override suspend fun downloadFileWithProgress(
            url: String,
            destinationFilePath: String,
        ): Flow<DownloadStatus> = error("Не используется в тестах")
    }

    @Test
    fun `writes only token email and auto login flag`() = runBlocking {
        val storage = MemoryStorage()
        val authorizer = AuthorizationImpl(FakeKtorClient(userEngine()), storage)
        authorizer.saveAuthData(AuthData(token = "token-1", cookies = "session=1"))
        authorizer.saveUser(userData)
        authorizer.saveAutoLogin(true)

        authorizer.persistAuthState()

        val keys = Json.parseToJsonElement(storage.saved!!).jsonObject.keys
        assertEquals(
            setOf("autoLogin", "token", "userEmail"),
            keys,
            "В файл должны попадать только токен, email и флаг авто-входа: ${storage.saved}",
        )
    }

    @Test
    fun `restores user data by email from server`() = runBlocking {
        val engine = userEngine()
        val storage = MemoryStorage().apply {
            saved = """{"autoLogin":true,"token":"token-1","userEmail":"user@example.com"}"""
        }
        val client = FakeKtorClient(engine)
        val authorizer = AuthorizationImpl(client, storage)
        client.tokenProvider = { runCatching { authorizer.getToken() }.getOrNull() }

        assertTrue(authorizer.restoreAuthState(), "Восстановление должно быть успешным")
        assertEquals(42, authorizer.getUserData().id, "Данные пользователя должны прийти с сервера")
        assertEquals("user@example.com", authorizer.getUserData().email, "Email должен прийти с сервера")
        assertEquals("token-1", authorizer.getToken(), "Токен должен восстановиться из файла")
        assertTrue(authorizer.getAutoLogin(), "Флаг авто-входа должен восстановиться из файла")
        assertEquals(1, engine.requestHistory.size, "Данные должны запрашиваться одним запросом")
        assertTrue(
            engine.requestHistory.single().url.toString().contains("users?email="),
            "Запрос должен идти по email пользователя",
        )
        assertEquals(
            "Bearer token-1",
            engine.requestHistory.single().headers[HttpHeaders.Authorization],
            "Запрос данных пользователя должен уйти с токеном из файла",
        )
    }

    @Test
    fun `clears state when saved session is incomplete`() = runBlocking {
        val storage = MemoryStorage().apply {
            saved = """{"autoLogin":true,"token":"token-1"}"""
        }
        val authorizer = AuthorizationImpl(FakeKtorClient(userEngine()), storage)

        assertFalse(authorizer.restoreAuthState(), "Сессия без email не должна восстанавливаться")
        assertNull(storage.saved, "Повреждённое состояние должно удаляться из хранилища")
    }

    @Test
    fun `clears state when server rejects restored token`() = runBlocking {
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val storage = MemoryStorage().apply {
            saved = """{"autoLogin":true,"token":"expired","userEmail":"user@example.com"}"""
        }
        val authorizer = AuthorizationImpl(FakeKtorClient(engine), storage)

        assertFalse(authorizer.restoreAuthState(), "Сессия с недействительным токеном не должна восстанавливаться")
        assertNull(storage.saved, "Неудачная сессия должна удаляться из хранилища")
    }

    @Test
    fun `clear removes saved session`() {
        val storage = MemoryStorage().apply { saved = """{"autoLogin":true,"token":"token-1"}""" }
        val authorizer = AuthorizationImpl(FakeKtorClient(userEngine()), storage)

        authorizer.clearAuthState()

        assertNull(storage.saved, "Сессия должна удаляться из хранилища")
    }

    private fun userEngine() = MockEngine {
        respond(
            content = USER_JSON,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private companion object {
        const val USER_JSON = """{"message":{"id":"42","name":"Иван","email":"user@example.com"}}"""

        val userData = UserData(
            id = 42,
            name = "Иван",
            lastName = null,
            patronymic = null,
            email = "user@example.com",
            accessLevel = AccessLevel.EMPLOYEE,
            role = UserRole.NONE,
            buildings = emptyList(),
            services = emptyList(),
        )
    }
}
