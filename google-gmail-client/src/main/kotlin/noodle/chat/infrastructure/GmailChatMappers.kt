package noodle.chat.infrastructure

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

