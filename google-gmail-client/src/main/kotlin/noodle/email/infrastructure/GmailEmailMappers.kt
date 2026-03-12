package noodle.email.infrastructure

import noodle.email.infrastructure.serialization.GmailHistory
import noodle.email.infrastructure.serialization.GmailMessage

fun GmailMessage.toEmailDomain() = noodle.email.domain.GmailMessage(id = id)

fun GmailHistory.Message.toEmailDomain() =
    noodle.email.domain.GmailHistory.Message(
        message = message.toEmailDomain(),
    )

fun GmailHistory.Change.toEmailDomain() =
    noodle.email.domain.GmailHistory.Change(
        messagesAdded = messagesAdded.map { it.toEmailDomain() },
    )

fun GmailHistory.toEmailDomain() =
    noodle.email.domain.GmailHistory(
        history = history.map { it.toEmailDomain() },
        historyId = historyId,
        nextPageToken = nextPageToken,
    )
