package noodle.finance.infrastructure

import noodle.email.infrastructure.serialization.GmailMessage

fun GmailMessage.toFinanceDomain() =
    noodle.finance.domain.GmailMessage(
        id = id,
        text = text,
        senderEmail = from?.address,
    )
