package noodle.chat.port.out

import noodle.chat.domain.StateToken

interface TokenRepository {
    suspend fun putToken(token: StateToken)
}
