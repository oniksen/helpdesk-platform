package data.repository

import AuthResponse
import Authorization
import MaxAuthorization
import MaxAuthorizationResult
import data.dto.AuthBody
import data.dto.response.AuthResponseDto
import data.network.KtorClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthorizationImpl(
    private val maxAuthorization: MaxAuthorization,
    private val client: KtorClient,
): Authorization {
    override suspend fun getMaxInitData(): MaxAuthorizationResult
        = maxAuthorization.getInitUserData()

    override suspend fun helpdeskAuth(maxInitData: String): AuthResponse {
        val clientInstance = client.instance()

        val response = clientInstance.post("https://helpdesk.lpmti.ru/max-app/v1/authorization") {
            contentType(ContentType.Application.Json)
            setBody(AuthBody(maxInitData))
        }

        if (response.status.isSuccess()) {
            /* Тело успешной авторизации.
            * {"cookies":"PHPSESSID=","bearer_token":"H4sIAAAAAAACAxXL2wqCMAAA0H\/Za0Fq6SToYcqcC7e8ZxGIKdFyJRFqGv27dd7PByRObnsU8zjniGGwBtey2l9MFzVlGLxSdB+C3nioXB9qoiSVzPpyyS1WiFhlGzD\/f5z5NMRRjuJfpxlDFqo1MlKsdbTgx2g1w0+\/vbmtT4TRjFt5Wii7wvPkmYhOQN2GQagd3g5UzFRW4DsB\/va6t5UAAAA=.561378ff843b1ade88740ba3ef10e177144208d8f4f608dadb2e10eac15e389e","user_id":2,"user_name":"\u0420\u043e\u043c\u0430\u043d","user_lastname":"\u0411\u0430\u0440\u0434\u0430\u043a\u043e\u0432","user_patronymic":"\u0412\u043b\u0430\u0434\u0438\u0441\u043b\u0430\u0432\u043e\u0432\u0438\u0447","services":[11,12,10,13,15,16,17,18],"buildings":[221,220,210,224,215,243,222,211,216,218,219,244,213,217,4844,223,214,212,228,1383]}
            * */

            val body = response.body<AuthResponseDto>()
            return AuthResponse.Success(
                cookie = body.cookies,
                bearerToken = body.bearerToken
            )
        } else {
            return AuthResponse.Error(
                message = response.bodyAsText().ifBlank { "Неизвестный ответ" }
            )
        }
    }
}