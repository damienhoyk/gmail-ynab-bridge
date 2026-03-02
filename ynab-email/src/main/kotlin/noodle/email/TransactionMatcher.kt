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

    private val accountIndex: Int
    private val amountIndex: Int
    private val dateIndex: Int
    private val payeeIndex: Int

    init {
        val fullOrder = listOf("ENTIRE_MATCH") + order
        accountIndex = fullOrder.indexOf(RegexGroup.ACCOUNT)
        amountIndex = fullOrder.indexOf(RegexGroup.AMOUNT)
        dateIndex = fullOrder.indexOf(RegexGroup.DATE)
        payeeIndex = fullOrder.indexOf(RegexGroup.PAYEE)
    }

    constructor(configuration: Configuration.Matcher) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)

    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val accountMatch = if (accountIndex != -1 && accountIndex < match.size) match[accountIndex] else null
        val amountMatch = if (amountIndex != -1 && amountIndex < match.size) match[amountIndex] else null
        val dateMatch = if (dateIndex != -1 && dateIndex < match.size) match[dateIndex] else null
        val payeeMatch = if (payeeIndex != -1 && payeeIndex < match.size) match[payeeIndex] else null

        if (amountMatch == null) {
            throw IllegalStateException()
        }

        if (dateMatch == null) {
            throw IllegalStateException()
        }

        // Optimize amount parsing to avoid string allocations and replacement
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

        val year = if (parsedDate.isSupported(YEAR)) parsedDate.get(YEAR) else systemDate.get(YEAR)
        val monthOfYear = if (parsedDate.isSupported(MONTH_OF_YEAR)) parsedDate.get(MONTH_OF_YEAR) else systemDate.get(MONTH_OF_YEAR)
        val dayOfMonth = if (parsedDate.isSupported(DAY_OF_MONTH)) parsedDate.get(DAY_OF_MONTH) else systemDate.get(DAY_OF_MONTH)

        val resolvedDate = LocalDate.of(year, monthOfYear, dayOfMonth)
        // Use LocalDate.toString() for ISO-8601 formatting (yyyy-MM-dd)
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