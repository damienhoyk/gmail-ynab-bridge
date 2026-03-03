package noodle.client

import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.coroutineScope
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

    fun gmailClient(user: String) = GmailClient {
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
                    }

                    BearerTokens(response.accessToken!!, response.refreshToken)
                }
            }
        }
    }

}