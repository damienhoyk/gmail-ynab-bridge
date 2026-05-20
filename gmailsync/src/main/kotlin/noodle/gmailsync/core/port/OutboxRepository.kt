package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Outbox as EmailOutbox

interface OutboxRepository {
    suspend fun putOutbox(outbox: EmailOutbox)
}
