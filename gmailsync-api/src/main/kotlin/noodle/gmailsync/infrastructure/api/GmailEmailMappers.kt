package noodle.gmailsync.infrastructure.api

import noodle.gmail.infrastructure.api.model.GmailHistory

fun GmailHistory.toAddedMessageIds(): List<String> = history.flatMap { it.messagesAdded }.mapNotNull { it.message.id }
