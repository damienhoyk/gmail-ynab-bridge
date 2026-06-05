package noodle.ynab.infrastructure.api.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

private val json = Json { ignoreUnknownKeys = true }

class YnabSerializationTests {
    @Test
    fun ynabUser() {
        val raw =
            """
            {
              "data": {
                "user": {
                  "id": "test-user-id-123"
                }
              }
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<YnabUser.Data>()), raw) as YnabUser.Data
        assertEquals("test-user-id-123", result.data.user.id)
    }

    @Test
    fun ynabBudget() {
        val raw =
            """
            {
              "data": {
                "budgets": [
                  {
                    "id": "budget-123",
                    "name": "My Budget"
                  },
                  {
                    "id": "budget-456",
                    "name": "Savings Budget"
                  }
                ]
              }
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<YnabBudget.Data>()), raw) as YnabBudget.Data
        assertEquals(2, result.data.budgets.size)
        assertEquals("budget-123", result.data.budgets[0].id)
        assertEquals("My Budget", result.data.budgets[0].name)
        assertEquals("budget-456", result.data.budgets[1].id)
        assertEquals("Savings Budget", result.data.budgets[1].name)
    }

    @Test
    fun ynabAccount() {
        val raw =
            """
            {
              "data": {
                "accounts": [
                  {
                    "id": "account-789",
                    "name": "Checking"
                  }
                ]
              }
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<YnabAccount.Data>()), raw) as YnabAccount.Data
        assertEquals(1, result.data.accounts.size)
        assertEquals("account-789", result.data.accounts[0].id)
        assertEquals("Checking", result.data.accounts[0].name)
    }

    @Test
    fun ynabTransactionWithSerialNames() {
        val raw =
            """
            {
              "data": {
                "transaction_ids": ["txn-111", "txn-222"],
                "transaction": {
                  "id": "txn-333",
                  "account_id": "account-xyz",
                  "amount": 25000,
                  "date": "2024-06-05",
                  "payee_name": "Gas Station"
                }
              }
            }
            """.trimIndent()
        val result = json.decodeFromString(serializer(typeOf<YnabTransaction.Data>()), raw) as YnabTransaction.Data
        assertEquals(2, result.data.transactionIds?.size)
        assertEquals("txn-111", result.data.transactionIds?.get(0))
        assertEquals("txn-222", result.data.transactionIds?.get(1))
        val transaction = result.data.transaction!!
        assertEquals("txn-333", transaction.id)
        assertEquals("account-xyz", transaction.accountId)
        assertEquals(25000, transaction.amount)
        assertEquals("2024-06-05", transaction.date)
        assertEquals("Gas Station", transaction.payeeName)
    }

    @Test
    fun ynabTransactionWithMissingOptionalFields() {
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
        val result = json.decodeFromString(serializer(typeOf<YnabTransaction.Data>()), raw) as YnabTransaction.Data
        assertNull(result.data.transactionIds)
        val transaction = result.data.transaction!!
        assertEquals("txn-444", transaction.id)
        assertNull(transaction.accountId)
        assertNull(transaction.amount)
        assertNull(transaction.date)
        assertNull(transaction.payeeName)
    }
}
