package data.repository

import AuthResponse
import Authorization
import KtorClient
import MaxAuthorization
import MaxAuthorizationResult
import auth.AuthData
import data.dto.auth.AuthBody
import data.dto.auth.response.AuthResponseDto
import data.dto.user.UserDataDto
import data.mapper.toDomain
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import user.UserData

class AuthorizationImpl(
    private val maxAuthorization: MaxAuthorization,
    client: KtorClient,
): Authorization {
    private val baseClient = client.baseInstance()
    private var userData: UserData? = null
    private var authData: AuthData? = null

    override suspend fun getMaxInitData(): MaxAuthorizationResult
        = maxAuthorization.getInitUserData()

    override suspend fun helpdeskAuth(maxInitData: String): AuthResponse {
        val response = baseClient.post("https://helpdesk.lpmti.ru/max-app/v1/authorization") {
            contentType(ContentType.Application.Json)
            setBody(AuthBody(maxInitData))
        }

        if (!response.status.isSuccess())
            error(response.bodyAsText().ifBlank { "Неизвестный ответ" })

        return response.body<AuthResponseDto>().toDomain()
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

    override fun getToken(): String =
        authData?.token ?: error("Токена не существует")

    override fun getUserData(): UserData =
        userData ?: error("Не найдена информация об авторизованном пользователе")
}