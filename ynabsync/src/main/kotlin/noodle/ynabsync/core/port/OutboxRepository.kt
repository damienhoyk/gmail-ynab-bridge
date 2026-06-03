package noodle.ynabsync.core.port

import kotlin.time.Duration

public interface OutboxRepository {
    public suspend fun updateTtl(
        destination: String,
        source: String,
        duration: Duration,
    ): Long
}
