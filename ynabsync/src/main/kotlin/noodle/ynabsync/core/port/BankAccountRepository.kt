package noodle.ynabsync.core.port

import noodle.ynabsync.core.domain.BankAccount

public interface BankAccountRepository {
    public suspend fun getAccounts(
        email: String,
        number: String,
    ): List<BankAccount>
}
