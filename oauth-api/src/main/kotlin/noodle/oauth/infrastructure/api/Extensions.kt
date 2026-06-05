package noodle.oauth.infrastructure.api

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.http.Parameters
import noodle.oauth.core.domain.LoadTokensCommand
import noodle.oauth.core.domain.OAuth2TokenRequest
import noodle.oauth.core.domain.RefreshTokensCommand
import noodle.oauth.core.service.TokenService

public fun AuthConfig.bearer(
    service: TokenService,
    loginId: String,
): Unit =
    bearer {
        loadTokens {
            val value = service.execute(LoadTokensCommand(loginId))
            BearerTokens(value, null)
        }

        refreshTokens {
            val value = service.execute(RefreshTokensCommand(loginId))
            BearerTokens(value, null)
        }
    }

public fun AuthConfig.bearer(accessToken: String): Unit =
    bearer {
        loadTokens { BearerTokens(accessToken, null) }
    }

public fun OAuth2TokenRequest.toForm(): Parameters =
    Parameters.build {
        code?.let { append("code", it) }
        clientId?.let { append("client_id", it) }
        clientSecret?.let { append("client_secret", it) }
        redirectUri?.let { append("redirect_uri", it) }
        refreshToken?.let { append("refresh_token", it) }
        grantType?.let { append("grant_type", it) }
    }
