package noodle.finance.port.out

import noodle.finance.domain.Bridge

interface BridgeRepository {
    suspend fun getBridge(
        source: String,
        destination: String,
    ): Bridge
}
