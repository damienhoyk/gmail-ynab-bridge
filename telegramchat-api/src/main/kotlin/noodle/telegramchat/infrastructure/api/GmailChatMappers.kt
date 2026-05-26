package noodle.telegramchat.infrastructure.api

import noodle.gmail.infrastructure.api.model.GmailProfile
import noodle.gmail.infrastructure.api.model.GmailWatch

fun GmailProfile.toChatDomain() =
    noodle.telegramchat.core.domain.GmailProfile(
        emailAddress = emailAddress,
        historyId = historyId,
    )

fun GmailWatch.toChatDomain() =
    noodle.telegramchat.core.domain.GmailWatch(
        historyId = historyId,
        expiration = expiration,
        error = error?.toChatDomain(),
    )

fun GmailWatch.Error.toChatDomain() =
    noodle.telegramchat.core.domain.GmailWatch.Error(
        code = code,
        message = message,
    )
