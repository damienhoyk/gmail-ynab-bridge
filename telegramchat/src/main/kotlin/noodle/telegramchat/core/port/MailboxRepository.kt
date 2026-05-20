package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.Mailbox

interface MailboxRepository {
    suspend fun updateMailbox(mailbox: Mailbox)
}
