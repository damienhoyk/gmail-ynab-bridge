package noodle.ynabsync.core.port

public interface BridgeRepository {
    public suspend fun getAccounts(
        mailAddress: String,
        destination: String,
    ): Map<String, String>
}
