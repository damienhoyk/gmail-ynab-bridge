package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.Bridge

public interface BridgeRepository {
    public suspend fun getBridges(mailAddress: String): List<Bridge>
}
