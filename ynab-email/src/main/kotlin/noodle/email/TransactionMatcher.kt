package noodle.email

import noodle.finance.YnabTransaction
import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField.*

class TransactionMatcher(
    val regex: Regex,
    val outgoing: Boolean = true,
    order: Set<RegexGroup>,
    inputDatePattern: String,
) {

    private val accountIndex: Int
    private val amountIndex: Int
    private val dateIndex: Int
    private val payeeIndex: Int

    init {
        val orderList = order.toList()
        accountIndex = orderList.indexOf(RegexGroup.ACCOUNT).let { if (it != -1) it + 1 else -1 }
        amountIndex = orderList.indexOf(RegexGroup.AMOUNT).let { if (it != -1) it + 1 else -1 }
        dateIndex = orderList.indexOf(RegexGroup.DATE).let { if (it != -1) it + 1 else -1 }
        payeeIndex = orderList.indexOf(RegexGroup.PAYEE).let { if (it != -1) it + 1 else -1 }
    }

    constructor(configuration: Configuration.Matcher) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)

    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val accountMatch = if (accountIndex != -1) match[accountIndex] else null
        val amountMatch = if (amountIndex != -1) match[amountIndex] else null
        val dateMatch = if (dateIndex != -1) match[dateIndex] else null
        val payeeMatch = if (payeeIndex != -1) match[payeeIndex] else null

        if (amountMatch == null) {
            throw IllegalStateException()
        }

        if (dateMatch == null) {
            throw IllegalStateException()
        }

        // Optimized currency parsing to avoid intermediate string allocations from replace()
        // Assumes transaction amount strings (e.g., '123.45') always contain exactly two decimal places.
        var milliunits = 0
        for (i in 0 until amountMatch.length) {
            val c = amountMatch[i]
            if (c in '0'..'9') {
                milliunits = milliunits * 10 + (c - '0')
            }
        }
        val amount = if (outgoing) -milliunits * 10 else milliunits * 10

        val parsedDate = inputDateFormatter.parse(dateMatch)
        val systemDate = LocalDate.now()

        // Optimized date component resolution to avoid list allocations
        val year = if (parsedDate.isSupported(YEAR)) parsedDate.get(YEAR) else systemDate.year
        val monthOfYear = if (parsedDate.isSupported(MONTH_OF_YEAR)) parsedDate.get(MONTH_OF_YEAR) else systemDate.monthValue
        val dayOfMonth = if (parsedDate.isSupported(DAY_OF_MONTH)) parsedDate.get(DAY_OF_MONTH) else systemDate.dayOfMonth

        val resolvedDate = LocalDate.of(year, monthOfYear, dayOfMonth)
        // LocalDate.toString() is faster than DateTimeFormatter for ISO-8601 formatting
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
