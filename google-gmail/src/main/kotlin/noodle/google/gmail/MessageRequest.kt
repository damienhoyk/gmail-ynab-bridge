package noodle.google.gmail

import io.ktor.client.request.*

data class MessageRequest(private val format: Format) {

    val block: HttpRequestBuilder.() -> Unit = {
        parameter("format", format.value)
    }

    enum class Format(val value: String) {
        MINIMAL("minimal"),
        FULL("full"),
        RAW("raw"),
        METADATA("metadata")
    }
}
