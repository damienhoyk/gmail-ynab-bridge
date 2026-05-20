package noodle.email.core.port

import noodle.email.core.domain.Outbox as EmailOutbox

interface OutboxRepository {
    suspend fun putOutbox(outbox: EmailOutbox)
}
