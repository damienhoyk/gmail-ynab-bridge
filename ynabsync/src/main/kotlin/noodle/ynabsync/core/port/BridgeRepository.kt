package noodle.ynabsync.core.port

public interface BridgeRepository {
    public suspend fun getAccounts(
        source: String,
        destination: String,
    ): Map<String, String>
}
