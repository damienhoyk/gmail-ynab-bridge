package noodle.email

import io.ktor.client.request.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class GmailWatchRequest(
    private val topicName: String? = null,
    private val labelIds: List<String> = emptyList(),
    private val labelFilterBehaviour: LabelFilterBehaviour = LabelFilterBehaviour.INCLUDE
) {

    val block: HttpRequestBuilder.() -> Unit = {
        setBody(
            buildJsonObject {
                put("topicName", topicName)
                put("labelFilterBehaviour", labelFilterBehaviour.value)
                putJsonArray("labelIds") {
                    labelIds.forEach { add(it) }
                }
            }
        )
    }

    enum class LabelFilterBehaviour(val value: String) {
        INCLUDE("include"),
        EXCLUDE("exclude")
    }
}
