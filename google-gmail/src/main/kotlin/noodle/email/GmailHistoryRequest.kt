package noodle.email

import io.ktor.client.request.*

data class GmailHistoryRequest(
    private val startHistoryId: Long,
    private val historyTypes: List<String> = emptyList(),
    private val labelIds: List<String> = emptyList()
) {

    val block: HttpRequestBuilder.() -> Unit = {
        parameter("startHistoryId", startHistoryId)
        historyTypes.forEach { parameter("historyTypes", it) }
        labelIds.forEach { parameter("labelId", it) }
    }

}
