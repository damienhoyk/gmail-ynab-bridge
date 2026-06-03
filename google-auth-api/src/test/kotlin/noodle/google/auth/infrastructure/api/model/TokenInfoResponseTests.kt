package noodle.google.auth.infrastructure.api.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

private val json = Json { ignoreUnknownKeys = true }

class TokenInfoResponseTests {
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
