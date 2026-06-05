package noodle.oauth2.infrastructure.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

private val json = Json { ignoreUnknownKeys = true }

class TokenSerializationTests {
    @Test
    fun tokenResponse() {
        val raw =
            """
            {
              "access_token": "abc",
              "id_token": "xyz",
              "refresh_token": "rfr",
              "expires_in": 3600
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<OAuth2TokenResponse>()), raw) as OAuth2TokenResponse
        assertEquals("abc", result.accessToken)
        assertEquals("xyz", result.idToken)
        assertEquals("rfr", result.refreshToken)
        assertEquals(3600, result.expiresIn)
        assertNull(result.error)
    }

    @Test
    fun tokenResponseSerialNames() {
        val raw = """{"access_token": "tok", "expires_in": 900}"""
        val result = json.decodeFromString(serializer(typeOf<OAuth2TokenResponse>()), raw) as OAuth2TokenResponse
        assertEquals("tok", result.accessToken)
        assertEquals(900, result.expiresIn)
        assertNull(result.idToken)
        assertNull(result.refreshToken)
    }

    @Test
    fun tokenResponseError() {
        val raw = """{"error": "invalid_grant"}"""
        val result = json.decodeFromString(serializer(typeOf<OAuth2TokenResponse>()), raw) as OAuth2TokenResponse
        assertEquals("invalid_grant", result.error)
        assertNull(result.accessToken)
    }

    @Test
    fun oidcDiscoveryDocument() {
        val raw =
            """
            {
              "issuer": "https://accounts.google.com",
              "authorization_endpoint": "https://accounts.google.com/o/oauth2/v2/auth",
              "token_endpoint": "https://oauth2.googleapis.com/token",
              "userinfo_endpoint": "https://openidconnect.googleapis.com/v1/userinfo",
              "jwks_uri": "https://www.googleapis.com/oauth2/v3/certs",
              "scopes_supported": ["openid", "email", "profile"],
              "response_types_supported": ["code", "token", "id_token"]
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<OidcDiscoveryDocument>()), raw) as OidcDiscoveryDocument
        assertEquals("https://oauth2.googleapis.com/token", result.tokenEndpoint)
    }
}
