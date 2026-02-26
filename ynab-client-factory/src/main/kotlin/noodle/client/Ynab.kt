package noodle.client

import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.coroutineScope
import noodle.finance.YnabClient
import noodle.security.AuthorizationRepository
import noodle.security.OAuth2TokenRequest
import noodle.security.TokenResponse
import noodle.security.YnabAuthClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class Ynab(
    private val id: String,
    private val secret: String,
    private val authorizationRepository: AuthorizationRepository,
    private val authClient: YnabAuthClient = YnabAuthClient()
) {

    suspend fun client(user: String) = coroutineScope {
        YnabClient {
            install(Auth) {
                bearer {
                    loadTokens {
                        val authorization = authorizationRepository.get(user)?.item()
                        val accessToken = authorization?.get("accessToken")?.s()
                        BearerTokens(accessToken!!, null)
                    }

                    refreshTokens {
                        val authorization = authorizationRepository.get(user)?.item()
                        val refreshToken = authorization?.get("refreshToken")?.s()

                        val authRequest = OAuth2TokenRequest(
                            grantType = "refresh_token",
                            clientId = id,
                            clientSecret = secret,
                            refreshToken = refreshToken
                        )

                        val response = authClient.getToken(authRequest).body<TokenResponse>()

                        val newAccessToken = response.accessToken
                        val newRefreshToken = response.refreshToken

                        newAccessToken?.let {
                            authorizationRepository.update(user) { put("accessToken", fromS(it)) }
                        }

                        newRefreshToken?.let {
                            authorizationRepository.update(user) { put("refreshToken", fromS(it)) }
                        }

                        BearerTokens(newAccessToken!!, newRefreshToken)
                    }
                }
            }
        }
    }

}