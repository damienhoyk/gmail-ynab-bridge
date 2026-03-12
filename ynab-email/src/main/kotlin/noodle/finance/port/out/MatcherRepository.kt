package noodle.finance.port.out

import noodle.finance.domain.TransactionMatcher

interface MatcherRepository {
    suspend fun queryMatcher(source: String): List<TransactionMatcher>
}
