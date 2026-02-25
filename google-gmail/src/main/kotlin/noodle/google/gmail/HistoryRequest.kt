package noodle.google.gmail

import io.ktor.client.request.*

data class HistoryRequest(
    private val startHistoryId: Long,
    private val historyTypes: List<String>,
    private val labelIds: List<String> = emptyList()
) {

    val block: HttpRequestBuilder.() -> Unit = {
        parameter("startHistoryId", startHistoryId)
        parameter("historyTypes", historyTypes.joinToString())
        labelIds.forEach { parameter("labelId", it) }
    }

}
