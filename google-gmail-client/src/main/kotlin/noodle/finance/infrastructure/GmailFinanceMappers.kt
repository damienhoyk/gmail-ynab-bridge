package noodle.finance.infrastructure

import noodle.email.infrastructure.serialization.GmailMessage

fun GmailMessage.toFinanceDomain() =
    noodle.finance.domain.GmailMessage(
        id = id,
        threadId = threadId,
        snippet = snippet,
        historyId = historyId,
        internalDate = internalDate,
        sizeEstimate = sizeEstimate,
        raw = raw,
        payload = payload?.toFinanceDomain(),
    )

fun GmailMessage.Part.toFinanceDomain(): noodle.finance.domain.GmailMessage.Part =
    noodle.finance.domain.GmailMessage.Part(
        partId = partId,
        mimeType = mimeType,
        parts = parts.map { it.toFinanceDomain() },
        headers = headers,
        body = body?.toFinanceDomain(),
    )

fun GmailMessage.Data.toFinanceDomain() = noodle.finance.domain.GmailMessage.Data(data = data)
