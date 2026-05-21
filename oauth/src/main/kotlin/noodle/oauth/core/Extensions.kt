package noodle.oauth.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

fun String.jsonObject() = Json.decodeFromString<JsonObject>(this)

val JsonObject.apiKey get() = get("apiKey")?.content
val JsonObject.clientId get() = get("clientId")?.content
val JsonObject.clientSecret get() = get("clientSecret")?.content

val JsonElement.content get() = jsonPrimitive.content
