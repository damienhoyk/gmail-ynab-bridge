package noodle.gmailsync.infrastructure.persistence

import kotlinx.coroutines.runBlocking
import noodle.gmailsync.core.domain.Outbox
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@TestInstance(PER_CLASS)
class DynamoDbOutboxRepositoryTests {
    private val repository = DynamoDbOutboxRepository(environment = "test")
    private val destination = "${UUID.randomUUID()}@app.ynab.com"
    private val address = "test-${UUID.randomUUID()}@gmail.com"
    private val mailId = UUID.randomUUID().toString()
    private val source = "$mailId:$address"

    @Test
    fun putOutbox(): Unit =
        runBlocking {
            val duration = 30.hours
            val before = Clock.System.now()
            repository.putOutbox(Outbox(destination, source), duration)
            val after = Clock.System.now()

            val response = repository.get(destination, source)
            val ttl = response.item()["ttl"]?.n()?.toLong()

            val expectedMin = (before + duration).epochSeconds
            val expectedMax = (after + duration).epochSeconds

            assertTrue(ttl != null && ttl in expectedMin..expectedMax, "ttl=$ttl should be in range [$expectedMin, $expectedMax]")
        }

    @AfterAll
    fun tearDown(): Unit =
        runBlocking {
            repository.delete(destination, source)
        }
}
