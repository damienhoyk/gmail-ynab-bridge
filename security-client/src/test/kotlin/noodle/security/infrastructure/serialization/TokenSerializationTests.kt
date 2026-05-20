package noodle.security.infrastructure.serialization

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
        val result = json.decodeFromString(serializer(typeOf<TokenResponse>()), raw) as TokenResponse
        assertEquals("abc", result.accessToken)
        assertEquals("xyz", result.idToken)
        assertEquals("rfr", result.refreshToken)
        assertEquals(3600, result.expiresIn)
        assertNull(result.error)
    }

    @Test
    fun tokenResponseSerialNames() {
        val raw = """{"access_token": "tok", "expires_in": 900}"""
        val result = json.decodeFromString(serializer(typeOf<TokenResponse>()), raw) as TokenResponse
        assertEquals("tok", result.accessToken)
        assertEquals(900, result.expiresIn)
        assertNull(result.idToken)
        assertNull(result.refreshToken)
    }

    @Test
    fun tokenResponseError() {
        val raw = """{"error": "invalid_grant"}"""
        val result = json.decodeFromString(serializer(typeOf<TokenResponse>()), raw) as TokenResponse
        assertEquals("invalid_grant", result.error)
        assertNull(result.accessToken)
    }

    @Test
    fun tokenInfoResponse() {
        val raw = """{"email": "user@example.com"}"""
        val result = json.decodeFromString(serializer(typeOf<TokenInfoResponse>()), raw) as TokenInfoResponse
        assertEquals("user@example.com", result.email)
    }

    @Test
    fun tokenInfoResponseNullEmail() {
        val raw = """{}"""
        val result = json.decodeFromString(serializer(typeOf<TokenInfoResponse>()), raw) as TokenInfoResponse
        assertNull(result.email)
    }
}
