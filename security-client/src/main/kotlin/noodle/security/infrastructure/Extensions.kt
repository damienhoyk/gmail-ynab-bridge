package noodle.security.infrastructure

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import noodle.security.core.domain.LoadTokensCommand
import noodle.security.core.domain.RefreshTokensCommand
import noodle.security.core.service.AuthTokenService

fun AuthConfig.bearer(
    service: AuthTokenService,
    loginId: String,
) = bearer {
    loadTokens {
        val value = service.execute(LoadTokensCommand(loginId))
        BearerTokens(value, null)
    }

    refreshTokens {
        val value = service.execute(RefreshTokensCommand(loginId))
        BearerTokens(value, null)
    }
}

fun AuthConfig.bearer(accessToken: String) =
    bearer {
        loadTokens { BearerTokens(accessToken, null) }
    }
