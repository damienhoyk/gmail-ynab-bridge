package noodle.chat.port.out

import noodle.chat.domain.Mailbox

interface MailboxRepository {
    suspend fun updateMailbox(mailbox: Mailbox)
}
