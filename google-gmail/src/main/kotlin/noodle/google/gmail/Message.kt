package noodle.google.gmail

import kotlinx.serialization.Serializable
import java.util.Base64
import kotlin.collections.ArrayDeque

private val urlDecoder = Base64.getUrlDecoder()

@Serializable
data class Message(
    val id: String? = null,
    val threadId: String? = null,
    val snippet: String? = null,
    val historyId: String? = null,
    val internalDate: String? = null,
    val sizeEstimate: Int? = null,
    val raw: String? = null,
    val payload: Part? = null
) {

    val parts
        get() = payload?.flatten() ?: emptyList()

    val text
        get() = parts
            .mapNotNull { it.data }
            .map { urlDecoder.decode(it) }
            // Optimization: stripHtml() already collapses all whitespace (including line breaks via \s+),
            // making a separate stripLineBreaks() call redundant.
            .joinToString(" ") { String(it).stripHtml().trim() }

    @Serializable
    data class Data(val data: String? = null)


    @Serializable
    data class List(val messages: kotlin.collections.List<Message> = emptyList(), val nextPageToken: String? = null, val resultSizeEstimate: Int)


    @Serializable
    data class Part(
        val partId: String,
        val mimeType: String,
        val parts: kotlin.collections.List<Part> = emptyList(),
        val body: Data? = null
    ) {

        val data
            get() = body?.data

        fun flatten(): kotlin.collections.List<Part> {
            val result = mutableListOf<Part>()
            val queue = ArrayDeque<Part>()

            queue.addLast(this)

            do {
                val current = queue.removeLast()
                // Optimization: Use asReversed() to avoid unnecessary list allocation and copy
                // while maintaining the correct order for DFS.
                queue.addAll(current.parts.asReversed())
                result.add(current)
            } while (queue.isNotEmpty())

            return result
        }
    }

}
