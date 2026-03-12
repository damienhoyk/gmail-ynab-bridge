package noodle.email.port.out

import noodle.email.domain.Mailbox

interface MailboxRepository {
    suspend fun getMailbox(address: String): Mailbox

    suspend fun putMailbox(mailbox: Mailbox)
}
