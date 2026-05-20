package noodle.finance.core.port

import noodle.finance.core.domain.TransactionMatcher

interface MatcherRepository {
    suspend fun queryMatcher(source: String): List<TransactionMatcher>
}
