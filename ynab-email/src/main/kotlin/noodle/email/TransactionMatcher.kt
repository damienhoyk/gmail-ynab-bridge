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

    constructor(configuration: Configuration.Matcher) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)

    // Pre-calculate indices to avoid O(n) lookups in parse()
    private val accountIndex = getIndex(RegexGroup.ACCOUNT, order)
    private val amountIndex = getIndex(RegexGroup.AMOUNT, order)
    private val dateIndex = getIndex(RegexGroup.DATE, order)
    private val payeeIndex = getIndex(RegexGroup.PAYEE, order)

    private fun getIndex(group: RegexGroup, order: Set<RegexGroup>): Int {
        val index = order.indexOf(group)
        return if (index != -1) index + 1 else -1 // +1 to account for group 0 (entire match)
    }

    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val accountMatch = if (accountIndex != -1) match[accountIndex] else null
        val amountMatch = if (amountIndex != -1) match[amountIndex] else null
        val dateMatch = if (dateIndex != -1) match[dateIndex] else null
        val payeeMatch = if (payeeIndex != -1) match[payeeIndex] else null

        if (amountMatch == null || dateMatch == null) {
            throw IllegalStateException()
        }

        // Fast currency parsing: avoid intermediate string allocations by removing '.' manually
        var mills = 0
        for (char in amountMatch) {
            if (char in '0'..'9') {
                mills = mills * 10 + (char - '0')
            }
        }
        mills *= 10 // Convert to milliunits (assuming 2 decimal places in input)
        val amount = if (outgoing) -mills else mills

        val parsedDate = inputDateFormatter.parse(dateMatch)
        val systemDate = LocalDate.now()

        // Direct property access instead of list mapping for date resolution
        val year = if (parsedDate.isSupported(YEAR)) parsedDate.get(YEAR) else systemDate.get(YEAR)
        val monthOfYear = if (parsedDate.isSupported(MONTH_OF_YEAR)) parsedDate.get(MONTH_OF_YEAR) else systemDate.get(MONTH_OF_YEAR)
        val dayOfMonth = if (parsedDate.isSupported(DAY_OF_MONTH)) parsedDate.get(DAY_OF_MONTH) else systemDate.get(DAY_OF_MONTH)

        val resolvedDate = LocalDate.of(year, monthOfYear, dayOfMonth)

        // Use LocalDate.toString() for standard ISO-8601 'yyyy-MM-dd' format as it is faster
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
