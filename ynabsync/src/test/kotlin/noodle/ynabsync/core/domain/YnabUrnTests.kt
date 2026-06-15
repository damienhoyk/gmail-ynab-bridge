package noodle.ynabsync.core.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YnabUrnTests {
    @Test
    fun parseValidUrn() {
        val input = "urn:app.ynab.com:user-123:budget:my-budget:account:acc-456"
        val urn = YnabUrn.parse(input)

        assertEquals("user-123", urn?.userId)
        assertEquals("my-budget", urn?.budgetId)
        assertEquals("acc-456", urn?.accountId)
    }

    @Test
    fun parseValidUrnWithDefaultBudget() {
        val input = "urn:app.ynab.com:alice:budget:default:account:ynab-account-id"
        val urn = YnabUrn.parse(input)

        assertEquals("alice", urn?.userId)
        assertEquals("default", urn?.budgetId)
        assertEquals("ynab-account-id", urn?.accountId)
    }

    @Test
    fun parseInvalidScheme() {
        val input = "http:app.ynab.com:user-123:budget:my-budget:account:acc-456"
        assertNull(YnabUrn.parse(input))
    }

    @Test
    fun parseMissingSegment() {
        val input = "urn:app.ynab.com:user-123:budget:my-budget"
        assertNull(YnabUrn.parse(input))
    }

    @Test
    fun parseEmptyString() {
        assertNull(YnabUrn.parse(""))
    }

    @Test
    fun parseBlankUserId() {
        val input = "urn:app.ynab.com::budget:my-budget:account:acc-456"
        assertNull(YnabUrn.parse(input))
    }

    @Test
    fun parseBlankBudgetId() {
        val input = "urn:app.ynab.com:user-123:budget::account:acc-456"
        assertNull(YnabUrn.parse(input))
    }

    @Test
    fun parseBlankAccountId() {
        val input = "urn:app.ynab.com:user-123:budget:my-budget:account:"
        assertNull(YnabUrn.parse(input))
    }
}
