package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.Mailbox

public interface MailboxRepository {
    public suspend fun updateMailbox(mailbox: Mailbox)
}
