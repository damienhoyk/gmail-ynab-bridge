package noodle.chat.core.port

import noodle.chat.core.domain.StateToken

interface TokenRepository {
    suspend fun putToken(token: StateToken)
}
