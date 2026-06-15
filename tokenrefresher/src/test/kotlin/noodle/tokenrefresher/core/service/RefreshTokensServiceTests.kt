package noodle.tokenrefresher.core.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import noodle.tokenrefresher.core.domain.RefreshableToken
import noodle.tokenrefresher.core.domain.TokenResponse
import noodle.tokenrefresher.core.port.OAuth2TokenProvider
import noodle.tokenrefresher.core.port.TokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class RefreshTokensServiceTests {
    @Test
    fun `refreshOne threads expiresIn into updateAccess`(): Unit =
        runBlocking {
            val testId = "user@example.com"
            val refreshToken = "refresh-token-123"
            val newAccessToken = "new-access-token"
            val expiresIn = 3600L
            val domain = "example.com"

            var updateAccessCalled = false
            var updateAccessId: String? = null
            var updateAccessValue: String? = null
            var updateAccessExpiresIn: Long? = null

            var updateRefreshCalled = false

            val fakeRepository =
                object : TokenRepository {
                    override fun findRefreshable(): Flow<List<RefreshableToken>> = flowOf()

                    override suspend fun updateAccess(
                        id: String,
                        value: String,
                        expiresIn: Long,
                    ) {
                        updateAccessCalled = true
                        updateAccessId = id
                        updateAccessValue = value
                        updateAccessExpiresIn = expiresIn
                    }

                    override suspend fun updateRefresh(
                        id: String,
                        value: String,
                    ) {
                        updateRefreshCalled = true
                    }
                }

            val fakeTokenResponse =
                TokenResponse(
                    accessToken = newAccessToken,
                    refreshToken = null,
                    expiresIn = expiresIn,
                )

            val fakeProvider: suspend (String) -> OAuth2TokenProvider? = { providedDomain ->
                if (providedDomain == domain) {
                    object : OAuth2TokenProvider {
                        override suspend fun refresh(refreshToken: String): TokenResponse = fakeTokenResponse
                    }
                } else {
                    null
                }
            }

            val service = RefreshTokensService(fakeRepository, fakeProvider)
            service.refreshOne(RefreshableToken(testId, refreshToken))

            assertEquals(true, updateAccessCalled, "updateAccess should be called")
            assertEquals(testId, updateAccessId, "updateAccess should receive correct id")
            assertEquals(newAccessToken, updateAccessValue, "updateAccess should receive new access token")
            assertEquals(expiresIn, updateAccessExpiresIn, "updateAccess should receive expiresIn from response")
            assertEquals(false, updateRefreshCalled, "updateRefresh should not be called when response has null refresh token")
        }

    @Test
    fun `refreshOne calls updateRefresh when response has non-blank refresh token`(): Unit =
        runBlocking {
            val testId = "user@example.com"
            val refreshToken = "refresh-token-123"
            val newAccessToken = "new-access-token"
            val newRefreshToken = "new-refresh-token"
            val expiresIn = 3600L
            val domain = "example.com"

            var updateRefreshCalled = false
            var updateRefreshId: String? = null
            var updateRefreshValue: String? = null

            val fakeRepository =
                object : TokenRepository {
                    override fun findRefreshable(): Flow<List<RefreshableToken>> = flowOf()

                    override suspend fun updateAccess(
                        id: String,
                        value: String,
                        expiresIn: Long,
                    ) {
                        // no-op for this test
                    }

                    override suspend fun updateRefresh(
                        id: String,
                        value: String,
                    ) {
                        updateRefreshCalled = true
                        updateRefreshId = id
                        updateRefreshValue = value
                    }
                }

            val fakeTokenResponse =
                TokenResponse(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    expiresIn = expiresIn,
                )

            val fakeProvider: suspend (String) -> OAuth2TokenProvider? = { providedDomain ->
                if (providedDomain == domain) {
                    object : OAuth2TokenProvider {
                        override suspend fun refresh(refreshToken: String): TokenResponse = fakeTokenResponse
                    }
                } else {
                    null
                }
            }

            val service = RefreshTokensService(fakeRepository, fakeProvider)
            service.refreshOne(RefreshableToken(testId, refreshToken))

            assertEquals(true, updateRefreshCalled, "updateRefresh should be called when response has non-blank refresh token")
            assertEquals(testId, updateRefreshId, "updateRefresh should receive correct id")
            assertEquals(newRefreshToken, updateRefreshValue, "updateRefresh should receive new refresh token")
        }

    @Test
    fun `refreshOne short-circuits when accessToken is null`(): Unit =
        runBlocking {
            val testId = "user@example.com"
            val refreshToken = "refresh-token-123"
            val domain = "example.com"

            var updateAccessCalled = false
            var updateRefreshCalled = false

            val fakeRepository =
                object : TokenRepository {
                    override fun findRefreshable(): Flow<List<RefreshableToken>> = flowOf()

                    override suspend fun updateAccess(
                        id: String,
                        value: String,
                        expiresIn: Long,
                    ) {
                        updateAccessCalled = true
                    }

                    override suspend fun updateRefresh(
                        id: String,
                        value: String,
                    ) {
                        updateRefreshCalled = true
                    }
                }

            val fakeTokenResponse =
                TokenResponse(
                    accessToken = null,
                    refreshToken = "new-refresh-token",
                    expiresIn = 3600L,
                )

            val fakeProvider: suspend (String) -> OAuth2TokenProvider? = { providedDomain ->
                if (providedDomain == domain) {
                    object : OAuth2TokenProvider {
                        override suspend fun refresh(refreshToken: String): TokenResponse = fakeTokenResponse
                    }
                } else {
                    null
                }
            }

            val service = RefreshTokensService(fakeRepository, fakeProvider)
            service.refreshOne(RefreshableToken(testId, refreshToken))

            assertEquals(false, updateAccessCalled, "updateAccess should not be called when accessToken is null")
            assertEquals(false, updateRefreshCalled, "updateRefresh should not be called when accessToken is null")
        }

    @Test
    fun `refreshOne short-circuits when accessToken is blank`(): Unit =
        runBlocking {
            val testId = "user@example.com"
            val refreshToken = "refresh-token-123"
            val domain = "example.com"

            var updateAccessCalled = false
            var updateRefreshCalled = false

            val fakeRepository =
                object : TokenRepository {
                    override fun findRefreshable(): Flow<List<RefreshableToken>> = flowOf()

                    override suspend fun updateAccess(
                        id: String,
                        value: String,
                        expiresIn: Long,
                    ) {
                        updateAccessCalled = true
                    }

                    override suspend fun updateRefresh(
                        id: String,
                        value: String,
                    ) {
                        updateRefreshCalled = true
                    }
                }

            val fakeTokenResponse =
                TokenResponse(
                    accessToken = "   ",
                    refreshToken = "new-refresh-token",
                    expiresIn = 3600L,
                )

            val fakeProvider: suspend (String) -> OAuth2TokenProvider? = { providedDomain ->
                if (providedDomain == domain) {
                    object : OAuth2TokenProvider {
                        override suspend fun refresh(refreshToken: String): TokenResponse = fakeTokenResponse
                    }
                } else {
                    null
                }
            }

            val service = RefreshTokensService(fakeRepository, fakeProvider)
            service.refreshOne(RefreshableToken(testId, refreshToken))

            assertEquals(false, updateAccessCalled, "updateAccess should not be called when accessToken is blank")
            assertEquals(false, updateRefreshCalled, "updateRefresh should not be called when accessToken is blank")
        }
}
