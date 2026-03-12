package noodle.chat.infrastructure

import noodle.email.infrastructure.serialization.GmailLabel
import noodle.email.infrastructure.serialization.GmailProfile
import noodle.email.infrastructure.serialization.GmailWatch

fun GmailProfile.toChatDomain() =
    noodle.chat.domain.GmailProfile(
        emailAddress = emailAddress,
        historyId = historyId,
    )

fun GmailWatch.toChatDomain() =
    noodle.chat.domain.GmailWatch(
        historyId = historyId,
        expiration = expiration,
        error = error?.toChatDomain(),
    )

fun GmailWatch.Error.toChatDomain() =
    noodle.chat.domain.GmailWatch.Error(
        code = code,
        message = message,
    )

fun GmailLabel.List.toChatDomain() =
    noodle.chat.domain.GmailLabel.List(
        labels = labels.map { it.toChatDomain() },
    )

fun GmailLabel.toChatDomain() =
    noodle.chat.domain.GmailLabel(
        id = id,
        name = name,
    )
