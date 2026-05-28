package noodle.bitwarden.infrastructure.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

fun String.bitwardenSecret(): JsonObject = Json.decodeFromString(this)

val JsonObject.apiKey get() = get("apiKey")?.content
val JsonObject.clientId get() = get("clientId")?.content
val JsonObject.clientSecret get() = get("clientSecret")?.content

private val JsonElement.content get() = jsonPrimitive.content
