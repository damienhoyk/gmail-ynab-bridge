package noodle.bitwarden.infrastructure.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@JvmInline
public value class BitwardenSecret(
    private val json: JsonObject,
) {
    public val apiKey: String? get() = json["apiKey"]?.content
    public val clientId: String? get() = json["clientId"]?.content
    public val clientSecret: String? get() = json["clientSecret"]?.content

    private val JsonElement.content: String get() = jsonPrimitive.content
}

public fun String.bitwardenSecret(): BitwardenSecret = BitwardenSecret(Json.decodeFromString(this))
