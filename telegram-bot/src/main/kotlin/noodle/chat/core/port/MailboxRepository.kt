package noodle.chat.core.port

import noodle.chat.core.domain.Mailbox

interface MailboxRepository {
    suspend fun updateMailbox(mailbox: Mailbox)
}
