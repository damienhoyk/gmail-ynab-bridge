package noodle.email.port.out

import noodle.email.domain.Outbox as EmailOutbox

interface OutboxRepository {
    suspend fun putOutbox(outbox: EmailOutbox)
}
