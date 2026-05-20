package noodle.ynabsync.core.domain

import kotlin.time.Duration.Companion.hours

data class Configuration(
    val matchers: List<Matcher> = emptyList(),
    val accounts: Map<String, String> = emptyMap(),
    val pollingInterval: Long = 1.hours.inWholeMilliseconds,
) {
    data class Matcher(
        val name: String,
        val pattern: String,
        val outgoing: Boolean = true,
        val order: LinkedHashSet<TransactionMatcher.RegexGroup>,
        val datePattern: String,
    )
}
