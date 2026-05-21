package noodle.gmailsync.infrastructure

import noodle.gmailsync.infrastructure.serialization.GmailHistory

fun GmailHistory.toAddedMessageIds(): List<String> =
    history.flatMap { it.messagesAdded }.mapNotNull { it.message.id }
