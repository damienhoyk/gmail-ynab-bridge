package noodle.home.security

import io.ktor.client.*
import io.ktor.client.request.*

abstract class OAuth2TokenProvider {

    abstract val httpClient: HttpClient
    abstract val tokenEndpoint: String
    open suspend fun getToken(request: OAuth2TokenRequest) = httpClient.post(tokenEndpoint, request.block)

}