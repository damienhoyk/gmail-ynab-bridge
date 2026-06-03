package noodle.gmail.infrastructure.api.model

import jakarta.mail.internet.InternetAddress
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

private val HTML_TAG_REGEX = "<[^>]*>".toRegex()
private val NON_BREAKING_SPACE_REGEX = "\\u00A0|&nbsp;".toRegex()
private val WHITESPACE_REGEX = "\\s+".toRegex()

private fun String.stripHtml() =
    replace(HTML_TAG_REGEX, "")
        .replace(NON_BREAKING_SPACE_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")

@Serializable
public data class GmailMessage(
    public val id: String? = null,
    public val threadId: String? = null,
    public val snippet: String? = null,
    public val historyId: String? = null,
    public val internalDate: String? = null,
    public val sizeEstimate: Int? = null,
    public val raw: String? = null,
    public val payload: Part? = null,
) {
    public val parts: kotlin.collections.List<Part> by lazy {
        payload?.flatten() ?: emptyList()
    }

    public val text: String by lazy {
        parts
            .asSequence()
            .mapNotNull { it.data }
            .map { Base64.UrlSafe.decode(it) }
            .joinToString(" ") { String(it).stripHtml().trim() }
    }

    public val from: InternetAddress? by lazy {
        payload
            ?.headers
            ?.find { it["name"].equals("from", true) }
            ?.get("value")
            ?.let(::InternetAddress)
    }

    @Serializable
    public data class Data(
        public val data: String? = null,
    )

    @Serializable
    public data class List(
        public val messages: kotlin.collections.List<GmailMessage> = emptyList(),
        public val nextPageToken: String? = null,
        public val resultSizeEstimate: Int,
    )

    @Serializable
    public data class Part(
        public val partId: String,
        public val mimeType: String,
        public val parts: kotlin.collections.List<Part> = emptyList(),
        public val headers: kotlin.collections.List<Map<String, String>> = emptyList(),
        public val body: Data? = null,
    ) {
        public val data: String?
            get() = body?.data

        public fun flatten(): kotlin.collections.List<Part> {
            val result = mutableListOf<Part>()
            val queue = ArrayDeque<Part>()

            queue.addLast(this)

            do {
                val current = queue.removeLast()
                queue.addAll(current.parts.asReversed())
                result.add(current)
            } while (queue.isNotEmpty())

            return result
        }
    }
}
