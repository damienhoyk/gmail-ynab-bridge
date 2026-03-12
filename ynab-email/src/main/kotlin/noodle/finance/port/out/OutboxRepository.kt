package noodle.finance.port.out

import kotlin.time.Duration

interface OutboxRepository {
    suspend fun updateTtl(
        destination: String,
        source: String,
        duration: Duration,
    ): Long
}
