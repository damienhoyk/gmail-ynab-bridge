package noodle.ynabsync.core.domain

import kotlin.time.Duration.Companion.hours

internal data class Configuration(
    internal val matchers: List<Matcher> = emptyList(),
    internal val accounts: Map<String, String> = emptyMap(),
    internal val pollingInterval: Long = 1.hours.inWholeMilliseconds,
) {
    internal data class Matcher(
        internal val name: String,
        internal val pattern: String,
        internal val outgoing: Boolean = true,
        internal val order: LinkedHashSet<TransactionMatcher.RegexGroup>,
        internal val datePattern: String,
    )
}
