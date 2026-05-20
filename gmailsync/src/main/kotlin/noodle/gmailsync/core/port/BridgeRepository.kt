package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Bridge

interface BridgeRepository {
    suspend fun queryBridge(source: String): List<Bridge>
}
