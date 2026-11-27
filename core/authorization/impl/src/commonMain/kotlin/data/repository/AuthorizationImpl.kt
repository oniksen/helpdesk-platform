package data.repository

import AuthResponse
import Authorization
import KtorClient
import auth.AuthData
import data.dto.auth.AuthBodyByMax
import data.dto.auth.AuthBodyStandard
import data.dto.auth.response.AuthResponseDto
import data.dto.authstate.AuthStateDto
import data.dto.user.UserDataDto
import data.mapper.toAuthData
import data.mapper.toDomain
import data.storage.AuthStateStorage
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import user.UserData

class AuthorizationImpl(
    client: KtorClient,
    private val storage: AuthStateStorage,
): Authorization {
    private val baseClient = client.baseInstance()
    private var userData: UserData? = null
    private var authData: AuthData? = null
    private var autoLogin: Boolean = false

    private companion object {
        val authStateJson = Json { ignoreUnknownKeys = true }
    }

    override suspend fun helpdeskAuth(maxInitData: String): AuthResponse {
        val response = baseClient.post("https://helpdesk.lpmti.ru/max-app/v1/authorization") {
            contentType(ContentType.Application.Json)
            setBody(AuthBodyByMax(maxInitData))
        }

        if (!response.status.isSuccess())
            error(response.bodyAsText().ifBlank { "Неизвестный ответ" })

        return response.body<AuthResponseDto>().toDomain()
    }

    override suspend fun helpdeskAuth(email: String, password: String): AuthResponse {
        val response = baseClient.post("https://helpdesk.lpmti.ru/help-desk/v3/auth") {
            contentType(ContentType.Application.Json)
            setBody(AuthBodyStandard("helpdesk_desktop_app", email, password))
        }

        if (!response.status.isSuccess())
            error(response.bodyAsText().ifBlank { "Неизвестный ответ" })

        return response.body<AuthResponseDto>().toDomain(false)
    }

    override suspend fun fetchUserData(email: String): UserData {
        val authUserData = baseClient
            .get("https://helpdesk.lpmti.ru/help-desk/v2/users?email=$email&api_key=helpdesk")

        if (!authUserData.status.isSuccess())
            error(authUserData.bodyAsText().ifBlank { "Неизвестный ответ" })

        return authUserData.body<UserDataDto>().toDomain()
    }

    override fun saveUser(userData: UserData) {
        this.userData = userData
    }

    override fun saveAuthData(authData: AuthData) {
        this.authData = authData
    }

    override fun saveAutoLogin(value: Boolean) {
        this.autoLogin = value
    }

    override fun getToken(): String =
        authData?.token ?: error("Токена не существует")

    override fun getUserData(): UserData =
        userData ?: error("Не найдена информация об авторизованном пользователе")

    override fun getAutoLogin(): Boolean {
        val stateJson = runCatching { storage.load() }.getOrNull() ?: return false
        val stateDto = runCatching { authStateJson.decodeFromString<AuthStateDto>(stateJson) }
            .getOrNull() ?: return false

        return stateDto.autoLogin
    }

    override fun persistAuthState() {
        val authData = this.authData ?: return
        val userData = this.userData ?: return

        val stateDto = AuthStateDto(
            autoLogin = this.autoLogin,
            token = authData.token,
            userEmail = userData.email,
        )

        storage.save(authStateJson.encodeToString(stateDto))
    }

    override suspend fun restoreAuthState(): Boolean {
        val stateJson = runCatching { storage.load() }.getOrNull() ?: return false
        val stateDto = runCatching { authStateJson.decodeFromString<AuthStateDto>(stateJson) }
            .getOrNull()
        val email = stateDto?.userEmail
        val restoredAuthData = stateDto?.toAuthData()
        val autoLoginFromState = stateDto?.autoLogin ?: false
        if (email == null || restoredAuthData == null) {
            clearFailedSession()
            return false
        }

        // Токен нужен до запроса данных: HTTP-клиент берёт его из текущего состояния.
        authData = restoredAuthData
        autoLogin = autoLoginFromState

        val restoredUserData = runCatching { fetchUserData(email) }.getOrNull()
        if (restoredUserData == null) {
            clearFailedSession()
            return false
        }

        userData = restoredUserData
        return true
    }

    override fun clearAuthState() {
        storage.clear()
    }

    /** Неудачная сессия не должна оставаться на диске и в памяти. */
    private fun clearFailedSession() {
        runCatching { clearAuthState() }
        authData = null
        userData = null
        autoLogin = false
    }
}