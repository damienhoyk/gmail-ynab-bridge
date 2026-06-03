package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.TransactionMatcher

public interface MatcherRepository {
    public suspend fun queryMatcher(source: String): List<TransactionMatcher>
}
