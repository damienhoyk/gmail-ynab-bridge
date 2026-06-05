package noodle.oauth2.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class OidcDiscoveryDocument(
    @SerialName("token_endpoint")
    public val tokenEndpoint: String?,
)
