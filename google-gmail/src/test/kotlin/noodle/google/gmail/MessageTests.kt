package noodle.google.gmail

import kotlin.test.Test
import kotlin.test.assertEquals


class MessageTests {

    @Test
    fun flatten() {
        val parts2 = listOf(
            Message.Part(partId = "2.1", mimeType = "text"),
            Message.Part(partId = "2.2", mimeType = "text")
        )

        val parts1 = listOf(
            Message.Part(partId = "1", mimeType = "text"),
            Message.Part(partId = "2", mimeType = "multipart", parts = parts2)
        )

        val root = Message.Part(partId = "root", mimeType = "multipart", parts = parts1)
        val flat = root.flatten()

        val expectedOrder = listOf("root", "1", "2", "2.1", "2.2")

        assertEquals(5, flat.count())
        assertEquals(expectedOrder, flat.map(Message.Part::partId))
    }

    @Test
    fun text() {
        val data = java.util.Base64.getUrlEncoder().encodeToString("  <p>Hello  \n  World</p> &nbsp; ".toByteArray())
        val part = Message.Part(partId = "1", mimeType = "text/html", body = Message.Data(data = data))
        val message = Message(payload = part)

        assertEquals("Hello World", message.text)
    }
}
