package noodle.email.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class GmailMessage(
    val id: String? = null,
    val threadId: String? = null,
    val snippet: String? = null,
    val historyId: String? = null,
    val internalDate: String? = null,
    val sizeEstimate: Int? = null,
    val raw: String? = null,
    val payload: Part? = null,
) {
    @Serializable
    data class Data(val data: String? = null)

    @Serializable
    data class List(
        val messages: kotlin.collections.List<GmailMessage> = emptyList(),
        val nextPageToken: String? = null,
        val resultSizeEstimate: Int,
    )

    @Serializable
    data class Part(
        val partId: String,
        val mimeType: String,
        val parts: kotlin.collections.List<Part> = emptyList(),
        val headers: kotlin.collections.List<Map<String, String>> = emptyList(),
        val body: Data? = null,
    )
}
