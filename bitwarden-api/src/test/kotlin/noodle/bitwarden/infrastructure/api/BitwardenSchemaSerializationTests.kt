package noodle.bitwarden.infrastructure.api

import com.bitwarden.sdk.schema.ClientSettings
import com.bitwarden.sdk.schema.SecretGetRequest
import com.bitwarden.sdk.schema.SecretResponse
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

// Guards reflect-config entries for com.bitwarden.sdk.schema.* classes.
// The Bitwarden SDK uses Jackson (via sdk-secrets transitive dependency) to
// serialize schema objects into JSON before passing them to the native library
// via JNA. allDeclaredFields + allDeclaredConstructors + allDeclaredMethods
// are required so Jackson can instantiate and populate these classes at native runtime.
//
// Getter naming note: Kotlin synthesizes property names by stripping "get" and lowercasing
// the first character. When the first two characters after "get" are both uppercase
// (e.g. getID() → iD, getAPIURL() → aPIURL) the result is non-intuitive and Kotlin
// resolves to the private backing field instead, causing a compilation error.
// Use explicit Java getter calls for those cases; use Kotlin property syntax for the rest.
private val mapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

class BitwardenSchemaSerializationTests {
    @Test
    fun deserializesClientSettings() {
        val json = """{"apiUrl":"https://api.bitwarden.com","identityUrl":"https://identity.bitwarden.com","userAgent":"gmail-ynab-bridge/1.0"}"""

        val settings = mapper.readValue(json, ClientSettings::class.java)

        assertEquals("https://api.bitwarden.com", settings.getAPIURL())
        assertEquals("https://identity.bitwarden.com", settings.identityURL)
        assertEquals("gmail-ynab-bridge/1.0", settings.userAgent)
    }

    @Test
    fun serializesClientSettingsRoundTrip() {
        val settings =
            ClientSettings().apply {
                setAPIURL("https://api.bitwarden.com")
                identityURL = "https://identity.bitwarden.com"
                userAgent = "gmail-ynab-bridge/1.0"
            }

        val json = mapper.writeValueAsString(settings)
        val deserialized = mapper.readValue(json, ClientSettings::class.java)

        assertEquals(settings.getAPIURL(), deserialized.getAPIURL())
        assertEquals(settings.identityURL, deserialized.identityURL)
        assertEquals(settings.userAgent, deserialized.userAgent)
    }

    @Test
    fun deserializesSecretGetRequest() {
        val id = UUID.randomUUID()
        val json = """{"id":"$id"}"""

        val request = mapper.readValue(json, SecretGetRequest::class.java)

        assertEquals(id, request.getID())
    }

    @Test
    fun serializesSecretGetRequestRoundTrip() {
        val id = UUID.randomUUID()
        val request = SecretGetRequest().apply { setID(id) }

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue(json, SecretGetRequest::class.java)

        assertEquals(id, deserialized.getID())
    }

    @Test
    fun deserializesSecretResponse() {
        val id = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val json =
            """
            {
              "id": "$id",
              "key": "MY_SECRET",
              "value": "supersecret",
              "note": "test note",
              "organizationId": "$orgId",
              "creationDate": "2026-05-14T10:00:00Z",
              "revisionDate": "2026-05-14T10:00:00Z"
            }
            """.trimIndent()

        val response = mapper.readValue(json, SecretResponse::class.java)

        assertEquals(id, response.getID())
        assertEquals("MY_SECRET", response.key)
        assertEquals("supersecret", response.value)
        assertEquals(orgId, response.organizationID)
        assertNotNull(response.creationDate)
    }

    @Test
    fun serializesSecretResponseRoundTrip() {
        val id = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val response =
            SecretResponse().apply {
                setID(id)
                key = "ROUND_TRIP_KEY"
                value = "round-trip-value"
                note = "round-trip note"
                organizationID = orgId
            }

        val json = mapper.writeValueAsString(response)
        val deserialized = mapper.readValue(json, SecretResponse::class.java)

        assertEquals(id, deserialized.getID())
        assertEquals("ROUND_TRIP_KEY", deserialized.key)
        assertEquals("round-trip-value", deserialized.value)
        assertEquals(orgId, deserialized.organizationID)
    }
}
