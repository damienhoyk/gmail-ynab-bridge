package noodle.finance.domain

import java.util.Base64

data class GmailMessage(
    val id: String? = null,
    val threadId: String? = null,
    val snippet: String? = null,
    val historyId: String? = null,
    val internalDate: String? = null,
    val sizeEstimate: Int? = null,
    val raw: String? = null,
    val payload: Part? = null,
    val status: Int? = null,
) {
    val parts
        get() = payload?.flatten() ?: emptyList()

    val text: String
        get() {
            val decoder = Base64.getUrlDecoder()
            return parts
                .asSequence()
                .mapNotNull { it.data }
                .joinToString(" ") { String(decoder.decode(it)).stripHtml().trim() }
        }

    data class Data(val data: String? = null)

    data class List(val messages: kotlin.collections.List<GmailMessage> = emptyList(), val nextPageToken: String? = null, val resultSizeEstimate: Int)

    data class Part(
        val partId: String,
        val mimeType: String,
        val parts: kotlin.collections.List<Part> = emptyList(),
        val headers: kotlin.collections.List<Map<String, String>> = emptyList(),
        val body: Data? = null,
    ) {
        val data
            get() = body?.data

        fun flatten(): kotlin.collections.List<Part> {
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
