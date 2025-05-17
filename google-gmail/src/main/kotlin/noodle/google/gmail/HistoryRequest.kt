package noodle.google.gmail

import io.ktor.client.request.*

data class HistoryRequest(
    private val startHistoryId: Long,
    private val historyTypes: List<String>,
    private val labelId: String
) {

    val block: HttpRequestBuilder.() -> Unit = {
        parameter("startHistoryId", startHistoryId)
        parameter("historyTypes", historyTypes.joinToString())
        parameter("labelId", labelId)
    }

}
