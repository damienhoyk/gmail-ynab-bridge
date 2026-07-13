package noodle.oauth.core.service

import kotlinx.coroutines.runBlocking
import noodle.oauth.core.domain.AuthorizeCommand
import noodle.oauth.core.domain.Login
import noodle.oauth.core.domain.LoginIdentity
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.Token
import noodle.oauth.core.domain.TokenResponse
import noodle.oauth.core.domain.User
import noodle.oauth.core.port.LoginIdProvider
import noodle.oauth.core.port.LoginRepository
import noodle.oauth.core.port.OAuth2TokenProvider
import noodle.oauth.core.port.TokenRepository
import noodle.oauth.core.port.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizeServiceTests {
    private fun createFakeTokenRepository(recordedUpdates: MutableList<TokenUpdate>): TokenRepository =
        object : TokenRepository {
            override suspend fun getUserId(state: String): String? = UUID.randomUUID().toString()

            override suspend fun getAccessToken(id: String): String? = null

            override suspend fun getRefreshToken(id: String): String? = null

            override suspend fun updateTokenValue(
                id: String,
                type: String,
                value: String,
                expiresIn: Long?,
            ): Token {
                recordedUpdates.add(TokenUpdate(id, type, value, expiresIn))
                return Token(id, type, value)
            }
        }

    private val fakeOAuth2TokenProvider =
        object : OAuth2TokenProvider {
            override suspend fun getToken(request: OAuth2TokenRequest): TokenResponse =
                TokenResponse(
                    accessToken = "fake-access-token-123",
                    refreshToken = "fake-refresh-token-456",
                    expiresIn = 3600,
                )
        }

    private val fakeLoginIdProvider =
        object : LoginIdProvider {
            override suspend fun getLoginId(tokenResponse: TokenResponse): LoginIdentity? =
                LoginIdentity(
                    id = "//user123@google.com",
                    aliases = listOf("//user@example.com"),
                )
        }

    private val fakeUserRepository =
        object : UserRepository {
            override suspend fun putUser(user: User) {}
        }

    private fun createFakeLoginRepository(recordedLogins: MutableList<Login>): LoginRepository =
        object : LoginRepository {
            override suspend fun putLogin(login: Login) {
                recordedLogins.add(login)
            }
        }

    @Test
    fun executeWritesAccessTokenWithExpiresIn() {
        runBlocking {
            val recordedUpdates = mutableListOf<TokenUpdate>()
            val recordedLogins = mutableListOf<Login>()
            val service =
                AuthorizeService(
                    clientId = "test-client-id",
                    clientSecret = "test-client-secret",
                    redirectUri = "http://localhost/callback",
                    authClient = { fakeOAuth2TokenProvider },
                    loginIdProvider = { fakeLoginIdProvider },
                    tokenRepository = { createFakeTokenRepository(recordedUpdates) },
                    userRepository = { fakeUserRepository },
                    loginRepository = { createFakeLoginRepository(recordedLogins) },
                    refreshTokenTtlSeconds = 5_184_000,
                )

            val command = AuthorizeCommand("test-code", "test-state")
            val statusCode = service.execute(command)

            assertEquals(200, statusCode)

            val accessUpdate = recordedUpdates.find { it.type == "access" }
            assertEquals("fake-access-token-123", accessUpdate?.value)
            assertEquals(3600L, accessUpdate?.expiresIn)
        }
    }

    @Test
    fun executeWritesRefreshTokenWithConfiguredTtl() {
        runBlocking {
            val recordedUpdates = mutableListOf<TokenUpdate>()
            val recordedLogins = mutableListOf<Login>()
            val refreshTtl = 7_776_000L
            val service =
                AuthorizeService(
                    clientId = "test-client-id",
                    clientSecret = "test-client-secret",
                    redirectUri = "http://localhost/callback",
                    authClient = { fakeOAuth2TokenProvider },
                    loginIdProvider = { fakeLoginIdProvider },
                    tokenRepository = { createFakeTokenRepository(recordedUpdates) },
                    userRepository = { fakeUserRepository },
                    loginRepository = { createFakeLoginRepository(recordedLogins) },
                    refreshTokenTtlSeconds = refreshTtl,
                )

            val command = AuthorizeCommand("test-code", "test-state")
            val statusCode = service.execute(command)

            assertEquals(200, statusCode)

            val refreshUpdate = recordedUpdates.find { it.type == "refresh" }
            assertEquals("fake-refresh-token-456", refreshUpdate?.value)
            assertEquals(refreshTtl, refreshUpdate?.expiresIn)
        }
    }

    @Test
    fun executeWithNullTokens() {
        runBlocking {
            val recordedUpdates = mutableListOf<TokenUpdate>()
            val recordedLogins = mutableListOf<Login>()
            val nullTokenProvider =
                object : OAuth2TokenProvider {
                    override suspend fun getToken(request: OAuth2TokenRequest): TokenResponse =
                        TokenResponse(
                            accessToken = null,
                            refreshToken = null,
                            expiresIn = null,
                        )
                }

            val service =
                AuthorizeService(
                    clientId = "test-client-id",
                    clientSecret = "test-client-secret",
                    redirectUri = "http://localhost/callback",
                    authClient = { nullTokenProvider },
                    loginIdProvider = { fakeLoginIdProvider },
                    tokenRepository = { createFakeTokenRepository(recordedUpdates) },
                    userRepository = { fakeUserRepository },
                    loginRepository = { createFakeLoginRepository(recordedLogins) },
                    refreshTokenTtlSeconds = 5_184_000,
                )

            val command = AuthorizeCommand("test-code", "test-state")
            val statusCode = service.execute(command)

            assertEquals(200, statusCode)
            assertEquals(0, recordedUpdates.size, "No token updates should be recorded when tokens are null")
        }
    }

    @Test
    fun executeWritesAliasLoginRow() {
        runBlocking {
            val recordedUpdates = mutableListOf<TokenUpdate>()
            val recordedLogins = mutableListOf<Login>()
            val service =
                AuthorizeService(
                    clientId = "test-client-id",
                    clientSecret = "test-client-secret",
                    redirectUri = "http://localhost/callback",
                    authClient = { fakeOAuth2TokenProvider },
                    loginIdProvider = { fakeLoginIdProvider },
                    tokenRepository = { createFakeTokenRepository(recordedUpdates) },
                    userRepository = { fakeUserRepository },
                    loginRepository = { createFakeLoginRepository(recordedLogins) },
                    refreshTokenTtlSeconds = 5_184_000,
                )

            val command = AuthorizeCommand("test-code", "test-state")
            val statusCode = service.execute(command)

            assertEquals(200, statusCode)

            val mainLogin = recordedLogins.find { it.id == "//user123@google.com" }
            assertEquals(1, recordedLogins.count { it.id == "//user123@google.com" })

            val aliasLogin = recordedLogins.find { it.id == "//user@example.com" }
            assertEquals("//user123@google.com", aliasLogin?.userId, "Alias login should point back to main login")
        }
    }

    data class TokenUpdate(
        val id: String,
        val type: String,
        val value: String,
        val expiresIn: Long?,
    )
}
