package noodle.bitwarden.infrastructure.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@JvmInline
value class BitwardenSecret(
    private val json: JsonObject,
) {
    val apiKey get() = json["apiKey"]?.content
    val clientId get() = json["clientId"]?.content
    val clientSecret get() = json["clientSecret"]?.content

    private val JsonElement.content get() = jsonPrimitive.content
}

fun String.bitwardenSecret(): BitwardenSecret = BitwardenSecret(Json.decodeFromString(this))
