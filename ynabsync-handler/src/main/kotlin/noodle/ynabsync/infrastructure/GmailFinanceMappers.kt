package noodle.ynabsync.infrastructure

import noodle.gmailsync.infrastructure.serialization.GmailMessage

fun GmailMessage.toFinanceDomain() =
    noodle.ynabsync.core.domain.GmailMessage(
        id = id,
        text = text,
        senderEmail = from?.address,
    )
