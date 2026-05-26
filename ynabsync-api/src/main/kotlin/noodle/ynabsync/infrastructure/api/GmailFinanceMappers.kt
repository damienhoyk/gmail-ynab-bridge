package noodle.ynabsync.infrastructure.api

import noodle.gmail.infrastructure.api.model.GmailMessage

fun GmailMessage.toFinanceDomain() =
    noodle.ynabsync.core.domain.GmailMessage(
        id = id,
        text = text,
        senderEmail = from?.address,
    )
