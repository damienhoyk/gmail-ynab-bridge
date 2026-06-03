package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Bridge

public interface BridgeRepository {
    public suspend fun queryBridge(source: String): List<Bridge>
}
