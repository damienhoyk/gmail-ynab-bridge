package noodle.email.port.out

import noodle.email.domain.Bridge

interface BridgeRepository {
    suspend fun queryBridges(source: String): List<Bridge>
}
