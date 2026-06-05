package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Outbox

public interface OutboxRepository {
    public suspend fun putOutbox(outbox: Outbox)
}
