package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.StateToken

interface TokenRepository {
    suspend fun putToken(token: StateToken)
}
