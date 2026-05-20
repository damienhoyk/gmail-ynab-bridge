package noodle.finance.core.domain

import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField.*

class TransactionMatcher(
    val regex: Regex,
    val outgoing: Boolean = true,
    val order: Set<RegexGroup>,
    inputDatePattern: String,
) {
    constructor(
        configuration: Configuration.Matcher,
    ) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern,
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)

    private val accountIndex =
        order.indexOf(RegexGroup.ACCOUNT).let { if (it >= 0) it + 1 else -1 }
    private val amountIndex =
        order.indexOf(RegexGroup.AMOUNT).let { if (it >= 0) it + 1 else -1 }
    private val dateIndex = order.indexOf(RegexGroup.DATE).let { if (it >= 0) it + 1 else -1 }
    private val payeeIndex = order.indexOf(RegexGroup.PAYEE).let { if (it >= 0) it + 1 else -1 }

    fun parse(input: String) =
        regex.find(input)?.groupValues?.let { match ->
            val amountMatch = if (amountIndex >= 0) match[amountIndex] else null
            val dateMatch = if (dateIndex >= 0) match[dateIndex] else null
            val accountMatch = if (accountIndex >= 0) match[accountIndex] else null
            val payeeMatch = if (payeeIndex >= 0) match[payeeIndex] else null

            if (dateMatch == null) {
                throw IllegalStateException("Date is required")
            }

            if (amountMatch == null) {
                throw IllegalStateException("Amount is required")
            }

            var mills = 0
            for (i in 0 until amountMatch.length) {
                val c = amountMatch[i]
                if (c in '0'..'9') {
                    mills = mills * 10 + (c - '0')
                }
            }

            mills *= 10
            val amount = if (outgoing) -mills else mills

            val parsedDate = inputDateFormatter.parse(dateMatch)
            val systemDate = LocalDate.now()

            val resolvedDate =
                LocalDate.of(
                    if (parsedDate.isSupported(YEAR)) {
                        parsedDate.get(YEAR)
                    } else {
                        systemDate.get(YEAR)
                    },
                    if (parsedDate.isSupported(MONTH_OF_YEAR)) {
                        parsedDate.get(MONTH_OF_YEAR)
                    } else {
                        systemDate.get(MONTH_OF_YEAR)
                    },
                    if (parsedDate.isSupported(DAY_OF_MONTH)) {
                        parsedDate.get(DAY_OF_MONTH)
                    } else {
                        systemDate.get(DAY_OF_MONTH)
                    },
                )
            val date = resolvedDate.toString()

            YnabTransaction(
                accountId = accountMatch,
                amount = amount,
                date = date,
                payeeName = payeeMatch,
            )
        }

    enum class RegexGroup {
        ACCOUNT,
        AMOUNT,
        DATE,
        PAYEE,
    }
}
