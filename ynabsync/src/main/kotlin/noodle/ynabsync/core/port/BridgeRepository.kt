package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.Bridge

interface BridgeRepository {
    suspend fun getBridge(
        source: String,
        destination: String,
    ): Bridge
}
