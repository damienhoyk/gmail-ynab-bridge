package noodle.ynab

import io.ktor.client.call.*
import kotlinx.coroutines.runBlocking
import noodle.home.security.BitwardenCredentialsProvider
import noodle.home.security.CachedAccessTokenProvider
import noodle.home.security.DynamoDbTokenStore
import noodle.home.security.SecretsManagerCredentialsProvider
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ClientTests {

    private val log = LoggerFactory.getLogger(javaClass)

    val ynabId = "1b93e434-c14d-4ed3-8108-f1aece36e595"
    val authClient = YnabAuthClient()
    val tokenStore = DynamoDbTokenStore()
    val bitwardenCredentialsProvider = SecretsManagerCredentialsProvider("bitwarden")
    val ynabCredentialsProvider = BitwardenCredentialsProvider("ynab", bitwardenCredentialsProvider)
    val ynabAccessTokenProvider = CachedAccessTokenProvider(ynabCredentialsProvider, tokenStore, authClient)
    val ynabClient = YnabClient(ynabId, ynabAccessTokenProvider)

    @Test
    fun getUser() {
        runBlocking { ynabClient.getUser() }
    }

    @Test
    fun getBudgets() {
        runBlocking { ynabClient.getBudgets().body<Budget.Data>() }
    }

    @Test
    fun getAccounts() {
        val accounts = runBlocking { ynabClient.getAccounts().body<Account.Data>() }
        accounts.data.accounts.forEach {
            log.info(it.toString())
        }
    }

    @Disabled
    @Test
    fun postTransactions() {
        val ynabTransactions = listOf(
            Transaction(
                accountId = "08e12562-9a53-4ebc-a4d2-085372038de7",
                payeeName = "test",
                amount = -3000,
                date = "2025-05-05"
            )
        )
        runBlocking {
            val body = Transaction.Body(transactions = ynabTransactions)
            ynabClient.postTransactions(request = TransactionsRequest(body))
        }
    }
}