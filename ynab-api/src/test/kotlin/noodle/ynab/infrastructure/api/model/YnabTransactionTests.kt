package noodle.ynab.infrastructure.api.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private val json = Json { ignoreUnknownKeys = true }

class YnabTransactionTests {
    @Test
    fun ynabTransactionNullOptionals() {
        val raw =
            """
            {
              "data": {
                "transaction_ids": null,
                "transaction": {
                  "id": "txn-444"
                }
              }
            }
            """.trimIndent()
        val result = json.decodeFromString<YnabTransaction.Data>(raw)
        assertNull(result.data.transactionIds)
        val transaction = result.data.transaction!!
        assertEquals("txn-444", transaction.id)
        assertNull(transaction.accountId)
        assertNull(transaction.amount)
        assertNull(transaction.date)
        assertNull(transaction.payeeName)
    }
}
