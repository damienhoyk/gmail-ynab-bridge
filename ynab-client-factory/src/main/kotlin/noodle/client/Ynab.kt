package noodle.client

import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import noodle.finance.YnabClient
import noodle.security.OAuth2TokenRequest
import noodle.security.TokenRepository
import noodle.security.TokenResponse
import noodle.security.YnabAuthClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS

class Ynab(
    private val id: String,
    private val secret: String,
    private val tokenRepository: TokenRepository,
    private val authClient: YnabAuthClient = YnabAuthClient()
) {

    fun client(user: String) = YnabClient {
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenRepository.get(user, "access")?.item()
                    val value = token?.get("value")?.s()
                    BearerTokens(value!!, null)
                }

                refreshTokens {
                    val token = tokenRepository.get(user, "refresh")?.item()
                    val value = token?.get("value")?.s()

                    val authRequest = OAuth2TokenRequest(
                        grantType = "refresh_token",
                        clientId = id,
                        clientSecret = secret,
                        refreshToken = value
                    )

                    val response = authClient.getToken(authRequest).body<TokenResponse>()

                    coroutineScope {
                        launch {
                            tokenRepository.update(user, "access") {
                                put("value", fromS(response.accessToken!!))
                            }
                        }
                        launch {
                            tokenRepository.update(user, "refresh") {
                                put("value", fromS(response.refreshToken!!))
                            }
                        }
                    }

                    BearerTokens(response.accessToken!!, response.refreshToken)
                }
            }
        }
    }

}