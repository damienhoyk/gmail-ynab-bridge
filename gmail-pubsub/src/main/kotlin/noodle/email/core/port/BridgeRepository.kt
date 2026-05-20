package noodle.email.core.port

import noodle.email.core.domain.Bridge

interface BridgeRepository {
    suspend fun queryBridge(source: String): List<Bridge>
}
