package noodle.home.security

import io.ktor.client.call.*
import kotlinx.coroutines.coroutineScope

class CachedAccessTokenProvider(
    private val credentialsProvider: CredentialsProvider,
    private val tokenStore: TokenStore,
    private val tokenProvider: OAuth2TokenProvider
) : AccessTokenProvider {

    override suspend fun getToken(id: String?) = coroutineScope {
        id ?: throw IllegalStateException("user specific access token requires id")

        val accessToken = tokenStore.getAccessToken(id)

        if (accessToken.isNullOrBlank()) {
            getNewToken(id)
        } else {
            accessToken
        }
    }

    override suspend fun getNewToken(id: String?) = coroutineScope {
        id ?: throw IllegalStateException("user specific access token requires id")

        credentialsProvider.load()

        val request = OAuth2TokenRequest(
            grantType = "refresh_token",
            clientId = credentialsProvider.getClientId()!!,
            clientSecret = credentialsProvider.getClientSecret(),
            refreshToken = tokenStore.getRefreshToken(id),
        )

        val response = tokenProvider.getToken(request).body<TokenResponse>()

        val accessToken = response.accessToken
        val refreshToken = response.refreshToken

        accessToken?.let { tokenStore.storeAccessToken(id, it) }
        refreshToken?.let { tokenStore.storeRefreshToken(id, it) }

        accessToken!!
    }

}