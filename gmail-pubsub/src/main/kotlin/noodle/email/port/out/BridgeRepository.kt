package noodle.email.port.out

import noodle.email.domain.Bridge

interface BridgeRepository {
    suspend fun queryBridge(source: String): List<Bridge>
}
