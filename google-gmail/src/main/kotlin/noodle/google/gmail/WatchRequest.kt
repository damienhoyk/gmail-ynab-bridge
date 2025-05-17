package noodle.google.gmail

import io.ktor.client.request.*

data class WatchRequest(
    private val topicName: String? = null,
    private val labelId: String? = null,
    private val labelFilterBehaviour: LabelFilterBehaviour = LabelFilterBehaviour.INCLUDE
) {

    val block: HttpRequestBuilder.() -> Unit = {
        setBody(
            mapOf(
                "topicName" to topicName,
                "labelIds" to labelId,
                "labelFilterBehavior" to labelFilterBehaviour.value
            ).filterValues { it != null }
        )
    }

    enum class LabelFilterBehaviour(val value: String) {
        INCLUDE("include"),
        EXCLUDE("exclude")
    }
}
