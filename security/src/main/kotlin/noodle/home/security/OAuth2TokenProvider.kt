package noodle.home.security

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Deferred

abstract class OAuth2TokenProvider {

    abstract val httpClient: HttpClient
    abstract val tokenEndpoint: Deferred<String>
    open suspend fun getToken(request: OAuth2TokenRequest) = httpClient.post(tokenEndpoint.await(), request.block)

}