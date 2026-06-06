package noodle.gmail.infrastructure.api.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private val json = Json { ignoreUnknownKeys = true }

class GmailWatchTests {
    @Test
    fun gmailWatchError() {
        val raw = """{"historyId": null, "expiration": null, "error": {"code": 403, "message": "forbidden"}}"""
        val result = json.decodeFromString<GmailWatch>(raw)
        assertNull(result.historyId)
        assertNull(result.expiration)
        assertEquals(403, result.error?.code)
        assertEquals("forbidden", result.error?.message)
    }

    @Test
    fun gmailWatchErrorType() {
        val raw = """{"code": 404, "message": "not found"}"""
        val result = json.decodeFromString<GmailWatch.Error>(raw)
        assertEquals(404, result.code)
        assertEquals("not found", result.message)
    }
}
