package noodle.home.security

import io.ktor.client.call.*
import kotlinx.coroutines.runBlocking

class CachedAccessTokenProvider(
    private val credentialsProvider: CredentialsProvider,
    private val tokenStore: TokenStore,
    private val tokenProvider: OAuth2TokenProvider
) : AccessTokenProvider {

    override fun getToken(id: String?): String {
        id ?: throw IllegalStateException("user specific access token requires id")

        val accessToken = tokenStore.getAccessToken(id)

        return if (accessToken.isNullOrBlank()) {
            getNewToken(id)
        } else {
            accessToken
        }
    }

    override fun getNewToken(id: String?): String {
        id ?: throw IllegalStateException("user specific access token requires id")

        credentialsProvider.load()

        val request = OAuth2TokenRequest(
            grantType = "refresh_token",
            clientId = credentialsProvider.clientId!!,
            clientSecret = credentialsProvider.clientSecret,
            refreshToken = tokenStore.getRefreshToken(id),
        )

        val response = runBlocking {
            tokenProvider.getToken(request).body<TokenResponse>()
        }

        val accessToken = response.accessToken
        val refreshToken = response.refreshToken

        accessToken?.let { tokenStore.storeAccessToken(id, it) }
        refreshToken?.let { tokenStore.storeRefreshToken(id, it) }

        return accessToken!!
    }

}