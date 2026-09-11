package data.network

import io.ktor.client.HttpClient

expect class KtorClient {
    fun instance(): HttpClient
}