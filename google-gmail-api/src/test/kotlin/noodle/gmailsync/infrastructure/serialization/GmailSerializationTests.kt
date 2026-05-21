package noodle.gmailsync.infrastructure.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

private val json = Json { ignoreUnknownKeys = true }

class GmailSerializationTests {
    @Test
    fun gmailProfile() {
        val raw = """{"emailAddress":"user@example.com","historyId":"42"}"""
        val result = json.decodeFromString(serializer(typeOf<GmailProfile>()), raw) as GmailProfile
        assertEquals("user@example.com", result.emailAddress)
        assertEquals(42L, result.historyId)
    }

    @Test
    fun gmailHistory() {
        val raw =
            """
            {
              "history": [{"messagesAdded": [{"message": {"id": "m1", "threadId": "t1"}}]}],
              "historyId": 99,
              "nextPageToken": "tok"
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<GmailHistory>()), raw) as GmailHistory
        assertEquals(99L, result.historyId)
        assertEquals("tok", result.nextPageToken)
        assertEquals("m1", result.history.first().messagesAdded.first().message.id)
    }

    @Test
    fun gmailHistoryChange() {
        val raw = """{"messagesAdded": [{"message": {"id": "m2", "threadId": "t2"}}]}"""
        val result = json.decodeFromString(serializer(typeOf<GmailHistory.Change>()), raw) as GmailHistory.Change
        assertEquals("m2", result.messagesAdded.first().message.id)
    }

    @Test
    fun gmailHistoryMessage() {
        val raw = """{"message": {"id": "m3", "threadId": "t3"}}"""
        val result = json.decodeFromString(serializer(typeOf<GmailHistory.Message>()), raw) as GmailHistory.Message
        assertEquals("m3", result.message.id)
    }

    @Test
    fun gmailLabel() {
        val raw = """{"id": "INBOX", "name": "Inbox"}"""
        val result = json.decodeFromString(serializer(typeOf<GmailLabel>()), raw) as GmailLabel
        assertEquals("INBOX", result.id)
        assertEquals("Inbox", result.name)
    }

    @Test
    fun gmailLabelList() {
        val raw = """{"labels": [{"id": "INBOX", "name": "Inbox"}, {"id": "SENT", "name": "Sent"}]}"""
        val result = json.decodeFromString(serializer(typeOf<GmailLabel.List>()), raw) as GmailLabel.List
        assertEquals(2, result.labels.size)
        assertEquals("INBOX", result.labels.first().id)
    }

    @Test
    fun gmailMessage() {
        val raw = """{"id": "m1", "threadId": "t1", "snippet": "hello", "historyId": "10"}"""
        val result = json.decodeFromString(serializer(typeOf<GmailMessage>()), raw) as GmailMessage
        assertEquals("m1", result.id)
        assertEquals("t1", result.threadId)
        assertEquals("hello", result.snippet)
    }

    @Test
    fun gmailMessageData() {
        val raw = """{"data": "aGVsbG8="}"""
        val result = json.decodeFromString(serializer(typeOf<GmailMessage.Data>()), raw) as GmailMessage.Data
        assertEquals("aGVsbG8=", result.data)
    }

    @Test
    fun gmailMessageList() {
        val raw = """{"messages": [{"id": "m1"}], "resultSizeEstimate": 1}"""
        val result = json.decodeFromString(serializer(typeOf<GmailMessage.List>()), raw) as GmailMessage.List
        assertEquals(1, result.resultSizeEstimate)
        assertEquals("m1", result.messages.first().id)
    }

    @Test
    fun gmailMessagePart() {
        val raw = """{"partId": "0", "mimeType": "text/plain", "body": {"data": "aGVsbG8="}, "headers": [{"name": "From", "value": "test@example.com"}]}"""
        val result = json.decodeFromString(serializer(typeOf<GmailMessage.Part>()), raw) as GmailMessage.Part
        assertEquals("0", result.partId)
        assertEquals("text/plain", result.mimeType)
        assertEquals("aGVsbG8=", result.data)
        assertEquals("From", result.headers.first()["name"])
    }

    @Test
    fun gmailWatch() {
        val raw = """{"historyId": 7, "expiration": 9999, "error": null}"""
        val result = json.decodeFromString(serializer(typeOf<GmailWatch>()), raw) as GmailWatch
        assertEquals(7L, result.historyId)
        assertEquals(9999L, result.expiration)
        assertNull(result.error)
    }

    @Test
    fun gmailWatchWithError() {
        val raw = """{"historyId": null, "expiration": null, "error": {"code": 403, "message": "forbidden"}}"""
        val result = json.decodeFromString(serializer(typeOf<GmailWatch>()), raw) as GmailWatch
        assertEquals(403, result.error?.code)
        assertEquals("forbidden", result.error?.message)
    }

    @Test
    fun gmailWatchError() {
        val raw = """{"code": 404, "message": "not found"}"""
        val result = json.decodeFromString(serializer(typeOf<GmailWatch.Error>()), raw) as GmailWatch.Error
        assertEquals(404, result.code)
        assertEquals("not found", result.message)
    }
}
