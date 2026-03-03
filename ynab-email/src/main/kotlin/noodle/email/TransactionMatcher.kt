package noodle.email

import noodle.finance.YnabTransaction
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

    companion object {
        private val fields = listOf(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    }

    constructor(configuration: Configuration.Matcher) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)

    // Pre-calculate regex group indexes to avoid repeated set creation and lookups in the parse method.
    private val groupIndexes = RegexGroup.entries.map { group ->
        val index = order.indexOf(group)
        if (index > -1) index + 1 else -1
    }

    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val (
            accountMatch,
            amountMatch,
            dateMatch,
            payeeMatch
        ) = groupIndexes.map { if (it > -1) match[it] else null }

        if (amountMatch == null) {
            throw IllegalStateException()
        }

        if (dateMatch == null) {
            throw IllegalStateException()
        }

        val mills = amountMatch.replace(".", "").toInt() * 10
        val amount =  if (outgoing) -mills else mills

        val parsedDate = inputDateFormatter.parse(dateMatch)
        val systemDate = LocalDate.now()

        val (
            year,
            monthOfYear,
            dayOfMonth
        ) = fields.map { if (parsedDate.isSupported(it)) parsedDate.get(it) else systemDate.get(it) }

        val resolvedDate = LocalDate.of(year, monthOfYear, dayOfMonth)
        // Optimized: LocalDate.toString() is faster than DateTimeFormatter for ISO-8601 'yyyy-MM-dd' format.
        val date = resolvedDate.toString()

        YnabTransaction(accountId = accountMatch, amount = amount, date = date, payeeName = payeeMatch)
    }

    enum class RegexGroup {
        ACCOUNT,
        AMOUNT,
        DATE,
        PAYEE
    }

}
