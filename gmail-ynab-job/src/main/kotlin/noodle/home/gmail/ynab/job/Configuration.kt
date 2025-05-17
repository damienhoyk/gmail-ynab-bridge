package noodle.home.gmail.ynab.job

import noodle.home.gmail.ynab.job.TransactionMatcher.RegexGroup
import kotlin.time.Duration.Companion.hours

data class Configuration(
    val matchers: List<Matcher> = emptyList(),
    val accounts: Map<String, String> = emptyMap(),
    val pollingInterval: Long = 1.hours.inWholeMilliseconds
) {

    data class Matcher(
        val name: String,
        val pattern: String,
        val outgoing: Boolean = true,
        val order: LinkedHashSet<RegexGroup>,
        val datePattern: String,
    )

}