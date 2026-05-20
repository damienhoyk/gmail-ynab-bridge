package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.TransactionMatcher

interface MatcherRepository {
    suspend fun queryMatcher(source: String): List<TransactionMatcher>
}
