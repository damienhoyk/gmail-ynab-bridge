package noodle.ynabsync.infrastructure

import noodle.ynabsync.core.domain.YnabTransactionsRequest
import noodle.ynabsync.infrastructure.serialization.YnabAccount
import noodle.ynabsync.infrastructure.serialization.YnabBudget
import noodle.ynabsync.infrastructure.serialization.YnabTransaction

fun YnabAccount.toFinanceDomain() =
    noodle.ynabsync.core.domain
        .YnabAccount(id = id, name = name)

fun YnabAccount.Body.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabAccount
        .Body(accounts = accounts.map { it.toFinanceDomain() })

fun YnabAccount.Data.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabAccount
        .Data(data = data.toFinanceDomain())

fun YnabBudget.toFinanceDomain() =
    noodle.ynabsync.core.domain
        .YnabBudget(id = id, name = name)

fun YnabBudget.Body.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabBudget
        .Body(budgets = budgets.map { it.toFinanceDomain() })

fun YnabBudget.Data.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabBudget
        .Data(data = data.toFinanceDomain())

fun YnabTransaction.toFinanceDomain() =
    noodle.ynabsync.core.domain
        .YnabTransaction(id = id, accountId = accountId, amount = amount)

fun YnabTransaction.Body.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabTransaction.Body(
        transactionIds = transactionIds,
        transaction = transaction?.toFinanceDomain(),
        transactions = transactions?.map { it.toFinanceDomain() },
    )

fun YnabTransaction.Data.toFinanceDomain() =
    noodle.ynabsync.core.domain.YnabTransaction
        .Data(data = data.toFinanceDomain())

fun YnabTransactionsRequest.YnabTransaction.toYnabData() =
    YnabTransaction(
        id = id,
        accountId = accountId,
        amount = amount,
        date = date,
        payeeName = payeeName,
    )

fun YnabTransactionsRequest.Body.toYnabData() =
    YnabTransaction.Body(
        transaction = transaction?.toYnabData(),
        transactions = transactions?.map { it.toYnabData() },
    )
