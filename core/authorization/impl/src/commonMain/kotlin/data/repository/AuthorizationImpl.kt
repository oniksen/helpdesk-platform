package data.repository

import AuthResponse
import Authorization
import KtorClient
import MaxAuthorization
import MaxAuthorizationResult
import auth.AuthData
import data.dto.auth.AuthBody
import data.dto.auth.response.AuthResponseDto
import data.dto.authstate.AuthStateDto
import data.dto.user.UserDataDto
import data.mapper.toAuthData
import data.mapper.toDomain
import data.mapper.toUserData
import data.storage.AuthStateStorage
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import user.UserData

class AuthorizationImpl(
    private val maxAuthorization: MaxAuthorization,
    client: KtorClient,
    private val storage: AuthStateStorage,
): Authorization {
    private val baseClient = client.baseInstance()
    private var userData: UserData? = null
    private var authData: AuthData? = null

    private companion object {
        val authStateJson = Json { ignoreUnknownKeys = true }
    }

    override suspend fun getMaxInitData(): MaxAuthorizationResult
        = maxAuthorization.getInitUserData()

    override suspend fun helpdeskAuth(maxInitData: String): AuthResponse {
        val response = baseClient.post("https://helpdesk.lpmti.ru/max-app/v1/authorization") {
            contentType(ContentType.Application.Json)
            setBody(AuthBody(maxInitData))
        }
        println("[DIAG] helpdeskAuth: POST status=${response.status.value}")

        if (!response.status.isSuccess())
            error(response.bodyAsText().ifBlank { "Неизвестный ответ" })

        return response.body<AuthResponseDto>().toDomain()
    }

    override suspend fun fetchUserData(email: String): UserData {
        val authUserData = baseClient
            .get("https://helpdesk.lpmti.ru/help-desk/v2/users?email=$email&api_key=helpdesk")
        println("[DIAG] fetchUserData: GET status=${authUserData.status.value}")

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

    override fun getToken(): String =
        authData?.token ?: error("Токена не существует")

    override fun getUserData(): UserData =
        userData ?: error("Не найдена информация об авторизованном пользователе")

    override fun persistAuthState() {
        val authData = this.authData ?: run {
            println("[DIAG] persistAuthState: skip (no authData)")
            return
        }
        val userData = this.userData ?: run {
            println("[DIAG] persistAuthState: skip (no userData)")
            return
        }

        val stateDto = AuthStateDto(
            maxInitData = authData.maxInitData,
            token = authData.token,
            cookies = authData.cookies,
            userId = userData.id,
            userName = userData.name,
            userLastName = userData.lastName,
            userPatronymic = userData.patronymic,
            userEmail = userData.email,
            accessLevel = userData.accessLevel.name,
            userRole = userData.role.name,
            buildings = userData.buildings,
            services = userData.services,
        )

        storage.save(authStateJson.encodeToString(stateDto))
        println("[DIAG] persistAuthState: saved")
    }

    override fun restoreAuthState(): Boolean {
        val stateJson = storage.load() ?: run {
            println("[DIAG] restoreAuthState: false (no stored state)")
            return false
        }
        val stateDto = runCatching { authStateJson.decodeFromString<AuthStateDto>(stateJson) }
            .getOrNull() ?: run {
            println("[DIAG] restoreAuthState: false (parse failed)")
            return false
        }

        val restoredAuthData = stateDto.toAuthData()
        val restoredUserData = stateDto.toUserData()
        if (restoredAuthData == null || restoredUserData == null) {
            println("[DIAG] restoreAuthState: false (mapper null auth=${restoredAuthData != null} user=${restoredUserData != null})")
            return false
        }

        authData = restoredAuthData
        userData = restoredUserData
        println("[DIAG] restoreAuthState: true")
        return true
    }
}