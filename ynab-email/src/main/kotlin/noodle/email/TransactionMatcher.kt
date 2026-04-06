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

    private val fields = listOf(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH)
    private val outputDatePattern = "yyyy-MM-dd"

    constructor(configuration: Configuration.Matcher) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)
    private val outputDateFormatter = DateTimeFormatter.ofPattern(outputDatePattern)

    private val accountIndex = order.indexOf(RegexGroup.ACCOUNT).let { if (it > -1) it + 1 else -1 }
    private val amountIndex = order.indexOf(RegexGroup.AMOUNT).let { if (it > -1) it + 1 else -1 }
    private val dateIndex = order.indexOf(RegexGroup.DATE).let { if (it > -1) it + 1 else -1 }
    private val payeeIndex = order.indexOf(RegexGroup.PAYEE).let { if (it > -1) it + 1 else -1 }

    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val accountMatch = if (accountIndex > -1) match[accountIndex] else null
        val amountMatch = if (amountIndex > -1) match[amountIndex] else null
        val dateMatch = if (dateIndex > -1) match[dateIndex] else null
        val payeeMatch = if (payeeIndex > -1) match[payeeIndex] else null

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
        val date = outputDateFormatter.format(resolvedDate)

        YnabTransaction(accountId = accountMatch, amount = amount, date = date, payeeName = payeeMatch)
    }

    enum class RegexGroup {
        ACCOUNT,
        AMOUNT,
        DATE,
        PAYEE
    }

}