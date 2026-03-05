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

    // Pre-calculate capturing group indices to avoid redundant work in the hot 'parse' path.
    // Index 0 is reserved for the entire match, so configuration indices are offset by 1.
    private val groupIndices = IntArray(RegexGroup.entries.size) { i ->
        val group = RegexGroup.entries[i]
        val indexInOrder = order.indexOf(group)
        if (indexInOrder != -1) indexInOrder + 1 else -1
    }

    /**
     * Parses the input string and returns a YnabTransaction if a match is found.
     * Optimized for performance by minimizing allocations and pre-calculating indices.
     */
    fun parse(input: String) = regex.find(input)?.groupValues?.let { match ->
        val accountMatch = getMatch(match, RegexGroup.ACCOUNT)
        val amountMatch = getMatch(match, RegexGroup.AMOUNT)
        val dateMatch = getMatch(match, RegexGroup.DATE)
        val payeeMatch = getMatch(match, RegexGroup.PAYEE)

        if (amountMatch == null || dateMatch == null) {
            throw IllegalStateException()
        }

        // Efficient currency parsing: avoids String.replace and multiple allocations.
        // It assumes the amount string follows a standard format where digits represent the value (e.g., "123.45" -> 12345).
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

        // Explicitly resolve date fields to avoid overhead of list mapping and destructuring.
        val year = if (parsedDate.isSupported(YEAR)) parsedDate.get(YEAR) else systemDate.get(YEAR)
        val monthOfYear = if (parsedDate.isSupported(MONTH_OF_YEAR)) parsedDate.get(MONTH_OF_YEAR) else systemDate.get(MONTH_OF_YEAR)
        val dayOfMonth = if (parsedDate.isSupported(DAY_OF_MONTH)) parsedDate.get(DAY_OF_MONTH) else systemDate.get(DAY_OF_MONTH)

        val resolvedDate = LocalDate.of(year, monthOfYear, dayOfMonth)

        // LocalDate.toString() is faster than DateTimeFormatter for standard ISO-8601 formatting.
        YnabTransaction(accountId = accountMatch, amount = amount, date = resolvedDate.toString(), payeeName = payeeMatch)
    }

    private fun getMatch(groupValues: List<String>, group: RegexGroup): String? {
        val index = groupIndices[group.ordinal]
        return if (index != -1 && index < groupValues.size) groupValues[index] else null
    }

    enum class RegexGroup {
        ACCOUNT,
        AMOUNT,
        DATE,
        PAYEE
    }

}