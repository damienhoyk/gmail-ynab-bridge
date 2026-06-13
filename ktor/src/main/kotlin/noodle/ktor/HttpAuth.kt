package noodle.ktor

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

/**
 * Installs a static bearer token with no refresh path, assuming a short-lived HttpClient (built per invocation).
 * A long-lived or reused client would serve a stale token.
 */
public fun AuthConfig.bearer(accessToken: String): Unit =
    bearer {
        loadTokens { BearerTokens(accessToken, null) }
    }
