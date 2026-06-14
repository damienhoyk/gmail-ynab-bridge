package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Outbox
import kotlin.time.Duration

public interface OutboxRepository {
    public suspend fun putOutbox(
        outbox: Outbox,
        duration: Duration,
    )
}
