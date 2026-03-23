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

    // Pre-calculate indices to avoid dynamic set allocations and lookups on every parse
    private val accountIndex: Int
    private val amountIndex: Int
    private val dateIndex: Int
    private val payeeIndex: Int

    init {
        val orderList = listOf("ENTIRE_MATCH") + order.toList()
        accountIndex = orderList.indexOf(RegexGroup.ACCOUNT)
        amountIndex = orderList.indexOf(RegexGroup.AMOUNT)
        dateIndex = orderList.indexOf(RegexGroup.DATE)
        payeeIndex = orderList.indexOf(RegexGroup.PAYEE)
    }

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