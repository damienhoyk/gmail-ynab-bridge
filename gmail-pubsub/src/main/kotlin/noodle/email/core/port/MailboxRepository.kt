package noodle.email.core.port

import noodle.email.core.domain.Mailbox

interface MailboxRepository {
    suspend fun getMailbox(address: String): Mailbox

    suspend fun putMailbox(mailbox: Mailbox)
}
