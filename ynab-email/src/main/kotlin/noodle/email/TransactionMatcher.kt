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

    constructor(configuration: Configuration.Matcher) : this(
        configuration.pattern.toRegex(),
        configuration.outgoing,
        configuration.order,
        configuration.datePattern
    )

    private val inputDateFormatter = DateTimeFormatter.ofPattern(inputDatePattern)

    // Optimization: Pre-calculate capturing group indices in the constructor to avoid redundant O(n) lookups in parse().
    // We add 1 to the index because match.groupValues[0] is the entire match.
    // Standard List.indexOf is used as the order set is indexed during initialization.
    private val accountIndex = (order.toList().indexOf(RegexGroup.ACCOUNT).takeIf { it >= 0 }?.plus(1)) ?: -1
    private val amountIndex = (order.toList().indexOf(RegexGroup.AMOUNT).takeIf { it >= 0 }?.plus(1)) ?: -1
    private val dateIndex = (order.toList().indexOf(RegexGroup.DATE).takeIf { it >= 0 }?.plus(1)) ?: -1
    private val payeeIndex = (order.toList().indexOf(RegexGroup.PAYEE).takeIf { it >= 0 }?.plus(1)) ?: -1

    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val amountMatch = if (amountIndex != -1) match[amountIndex] else null
        val dateMatch = if (dateIndex != -1) match[dateIndex] else null

        if (amountMatch == null || dateMatch == null) {
            throw IllegalStateException("Required fields (AMOUNT, DATE) not found in match")
        }

        val accountMatch = if (accountIndex != -1) match[accountIndex] else null
        val payeeMatch = if (payeeIndex != -1) match[payeeIndex] else null

        // Optimization: Manual currency parsing to milliunits avoids String.replace() and intermediate allocations.
        // This handles a potential leading negative sign and assumes the string contains exactly two decimal places.
        var mills = 0
        var isNegative = false
        for (i in 0 until amountMatch.length) {
            val char = amountMatch[i]
            when {
                char in '0'..'9' -> mills = mills * 10 + (char - '0')
                char == '-' -> isNegative = true
            }
        }
        if (isNegative) mills = -mills
        mills *= 10
        val amount = if (outgoing) -mills else mills

        val parsedDate = inputDateFormatter.parse(dateMatch)
        val systemDate = LocalDate.now()

        // Optimization: Unrolling the date field resolution to avoid list allocation and mapping.
        val year = if (parsedDate.isSupported(YEAR)) parsedDate.get(YEAR) else systemDate.get(YEAR)
        val monthOfYear = if (parsedDate.isSupported(MONTH_OF_YEAR)) parsedDate.get(MONTH_OF_YEAR) else systemDate.get(MONTH_OF_YEAR)
        val dayOfMonth = if (parsedDate.isSupported(DAY_OF_MONTH)) parsedDate.get(DAY_OF_MONTH) else systemDate.get(DAY_OF_MONTH)

        val resolvedDate = LocalDate.of(year, monthOfYear, dayOfMonth)
        // Optimization: LocalDate.toString() is more efficient than DateTimeFormatter for standard 'yyyy-MM-dd' format.
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
