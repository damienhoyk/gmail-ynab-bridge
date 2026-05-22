package noodle.oauth.infrastructure

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import noodle.oauth.core.domain.LoadTokensCommand
import noodle.oauth.core.domain.RefreshTokensCommand
import noodle.oauth.core.service.TokenService

fun AuthConfig.bearer(
    service: TokenService,
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
