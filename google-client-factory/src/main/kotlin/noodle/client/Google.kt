package noodle.client

import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import noodle.email.GmailClient
import noodle.security.GoogleAuthClient
import noodle.security.OAuth2TokenRequest
import noodle.security.TokenRepository
import noodle.security.TokenResponse
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class Google(
    private val id: String,
    private val secret: String,
    private val tokenRepository: TokenRepository,
    private val authClient: GoogleAuthClient = GoogleAuthClient()
) {

    suspend fun gmailClient(user: String) = coroutineScope {
        GmailClient {
            install(Auth) {
                bearer {
                    loadTokens {
                        val authorization = tokenRepository.get(user, "access")?.item()
                        val accessToken = authorization?.get("value")?.s()
                        BearerTokens(accessToken!!, null)
                    }

                    refreshTokens {
                        val authorization = tokenRepository.get(user, "refresh")?.item()
                        val refreshToken = authorization?.get("value")?.s()

                        val authRequest = OAuth2TokenRequest(
                            grantType = "refresh_token",
                            clientId = id,
                            clientSecret = secret,
                            refreshToken = refreshToken
                        )

                        val response = authClient.getToken(authRequest).body<TokenResponse>()

                        val newAccessToken = response.accessToken
                        val newRefreshToken = response.refreshToken

                        val job1 = newAccessToken?.let {
                            launch {
                                tokenRepository.update(user, "access") { put("value", fromS(it)) }
                            }
                        }

                        val job2 = newRefreshToken?.let {
                            launch {
                                tokenRepository.update(user, "refresh") { put("value", fromS(it)) }
                            }
                        }

                        listOfNotNull(job1, job2).joinAll()


                        BearerTokens(newAccessToken!!, newRefreshToken)
                    }
                }
            }
        }
    }

}