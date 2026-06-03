package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.StateToken

public interface TokenRepository {
    public suspend fun putToken(token: StateToken)
}
