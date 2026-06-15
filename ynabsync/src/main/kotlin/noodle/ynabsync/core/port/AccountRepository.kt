package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.Account

public interface AccountRepository {
    public suspend fun getAccounts(owner: String): List<Account>
}
