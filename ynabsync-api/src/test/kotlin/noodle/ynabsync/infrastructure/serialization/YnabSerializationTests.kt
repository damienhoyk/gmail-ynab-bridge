package noodle.ynabsync.infrastructure.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import noodle.ynab.infrastructure.api.model.YnabAccount
import noodle.ynab.infrastructure.api.model.YnabBudget
import noodle.ynab.infrastructure.api.model.YnabTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

private val json = Json { ignoreUnknownKeys = true }

class YnabSerializationTests {
    @Test
    fun ynabBudget() {
        val raw = """{"id": "b1", "name": "My Budget"}"""
        val result = json.decodeFromString(serializer(typeOf<YnabBudget>()), raw) as YnabBudget
        assertEquals("b1", result.id)
        assertEquals("My Budget", result.name)
    }

    @Test
    fun ynabBudgetBody() {
        val raw = """{"budgets": [{"id": "b1", "name": "My Budget"}]}"""
        val result = json.decodeFromString(serializer(typeOf<YnabBudget.Body>()), raw) as YnabBudget.Body
        assertEquals(1, result.budgets.size)
        assertEquals("b1", result.budgets.first().id)
    }

    @Test
    fun ynabBudgetData() {
        val raw = """{"data": {"budgets": [{"id": "b2", "name": "Work"}]}}"""
        val result = json.decodeFromString(serializer(typeOf<YnabBudget.Data>()), raw) as YnabBudget.Data
        assertEquals(
            "b2",
            result.data.budgets
                .first()
                .id,
        )
    }

    @Test
    fun ynabTransaction() {
        val raw = """{"id": "tx1", "account_id": "acc1", "amount": -5000, "date": "2026-05-14", "payee_name": "Coffee"}"""
        val result = json.decodeFromString(serializer(typeOf<YnabTransaction>()), raw) as YnabTransaction
        assertEquals("tx1", result.id)
        assertEquals("acc1", result.accountId)
        assertEquals(-5000, result.amount)
        assertEquals("2026-05-14", result.date)
        assertEquals("Coffee", result.payeeName)
    }

    @Test
    fun ynabTransactionSerialNames() {
        val raw = """{"account_id": "acc2", "payee_name": "Merchant"}"""
        val result = json.decodeFromString(serializer(typeOf<YnabTransaction>()), raw) as YnabTransaction
        assertEquals("acc2", result.accountId)
        assertEquals("Merchant", result.payeeName)
        assertNull(result.id)
    }

    @Test
    fun ynabTransactionBody() {
        val raw = """{"transaction": {"id": "tx1", "account_id": "acc1"}}"""
        val result = json.decodeFromString(serializer(typeOf<YnabTransaction.Body>()), raw) as YnabTransaction.Body
        assertEquals("tx1", result.transaction?.id)
    }

    @Test
    fun ynabTransactionData() {
        val raw = """{"data": {"transaction": {"id": "tx2", "account_id": "acc2"}}}"""
        val result = json.decodeFromString(serializer(typeOf<YnabTransaction.Data>()), raw) as YnabTransaction.Data
        assertEquals("tx2", result.data.transaction?.id)
    }

    @Test
    fun ynabAccount() {
        val raw = """{"id": "acc1", "name": "Checking"}"""
        val result = json.decodeFromString(serializer(typeOf<YnabAccount>()), raw) as YnabAccount
        assertEquals("acc1", result.id)
        assertEquals("Checking", result.name)
    }

    @Test
    fun ynabAccountBody() {
        val raw = """{"accounts": [{"id": "acc1", "name": "Checking"}]}"""
        val result = json.decodeFromString(serializer(typeOf<YnabAccount.Body>()), raw) as YnabAccount.Body
        assertEquals(1, result.accounts.size)
        assertEquals("acc1", result.accounts.first().id)
    }

    @Test
    fun ynabAccountData() {
        val raw = """{"data": {"accounts": [{"id": "acc2", "name": "Savings"}]}}"""
        val result = json.decodeFromString(serializer(typeOf<YnabAccount.Data>()), raw) as YnabAccount.Data
        assertEquals(
            "acc2",
            result.data.accounts
                .first()
                .id,
        )
    }
}
