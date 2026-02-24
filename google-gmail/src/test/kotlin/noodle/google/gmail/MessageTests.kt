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
    fun clean() {
        val rawHtml = """
            <html>
                <body>
                    Hello&nbsp;World!
                    <br>
                    This is a   test.
                </body>
            </html>
        """.trimIndent()

        val cleaned = rawHtml.stripHtml().trim()

        assertEquals("Hello World! This is a test.", cleaned)
    }

    @Test
    fun stripLineBreaks() {
        val input = "Line 1\r\nLine 2\nLine 3"
        val cleaned = input.stripLineBreaks()
        assertEquals("Line 1 Line 2 Line 3", cleaned)
    }
}
