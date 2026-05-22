package noodle.ynabsync.core.port

interface BridgeRepository {
    suspend fun getAccounts(
        source: String,
        destination: String,
    ): Map<String, String>
}
