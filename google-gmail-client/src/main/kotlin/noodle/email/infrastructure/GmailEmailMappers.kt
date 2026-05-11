package noodle.email.infrastructure

import noodle.email.infrastructure.serialization.GmailHistory

fun GmailHistory.toAddedMessageIds(): List<String> =
    history.flatMap { it.messagesAdded }.mapNotNull { it.message.id }
