package noodle.finance.core.port

import noodle.finance.core.domain.Bridge

interface BridgeRepository {
    suspend fun getBridge(
        source: String,
        destination: String,
    ): Bridge
}
