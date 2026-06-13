package noodle.ktor

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

public fun AuthConfig.bearer(accessToken: String): Unit =
    bearer {
        loadTokens { BearerTokens(accessToken, null) }
    }
