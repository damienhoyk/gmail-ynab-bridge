package noodle.gmailsync.core.port

import noodle.gmailsync.core.domain.Mailbox

interface MailboxRepository {
    suspend fun getMailbox(address: String): Mailbox

    suspend fun putMailbox(mailbox: Mailbox)
}
