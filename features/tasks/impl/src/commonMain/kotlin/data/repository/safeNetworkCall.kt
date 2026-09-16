package data.repository

import domain.exception.AppException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.io.IOException

// Универсальный перехватчик ошибок для Ktor
inline fun <T> safeNetworkCall(block: () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (e: HttpRequestTimeoutException) {
        NetworkResult.Error(AppException.Network.Timeout())
    } catch (e: ClientRequestException) {
        NetworkResult.Error(AppException.Network.ServerError(e.response.status.value, e.message))
    } catch (e: ServerResponseException) {
        NetworkResult.Error(AppException.Network.ServerError(e.response.status.value, e.message))
    } catch (e: IOException) {
        NetworkResult.Error(AppException.Network.NoInternet())
    } catch (e: Throwable) {
        NetworkResult.Error(AppException.Unknown(e))
    }
}
