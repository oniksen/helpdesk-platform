package data.repository

import domain.exception.AppException

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T): NetworkResult<T>()
    data class Error(val exception: AppException): NetworkResult<Nothing>()
}