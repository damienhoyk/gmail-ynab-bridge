package noodle.finance.infrastructure

import noodle.finance.domain.YnabTransactionsRequest
import noodle.finance.infrastructure.serialization.YnabAccount
import noodle.finance.infrastructure.serialization.YnabBudget
import noodle.finance.infrastructure.serialization.YnabTransaction

fun YnabAccount.toFinanceDomain() = noodle.finance.core.domain.YnabAccount(id = id, name = name)

fun YnabAccount.Body.toFinanceDomain() =
    noodle.finance.core.domain.YnabAccount.Body(accounts = accounts.map { it.toFinanceDomain() })

fun YnabAccount.Data.toFinanceDomain() =
    noodle.finance.core.domain.YnabAccount.Data(data = data.toFinanceDomain())

fun YnabBudget.toFinanceDomain() = noodle.finance.core.domain.YnabBudget(id = id, name = name)

fun YnabBudget.Body.toFinanceDomain() =
    noodle.finance.core.domain.YnabBudget.Body(budgets = budgets.map { it.toFinanceDomain() })

fun YnabBudget.Data.toFinanceDomain() =
    noodle.finance.core.domain.YnabBudget.Data(data = data.toFinanceDomain())

fun YnabTransaction.toFinanceDomain() =
    noodle.finance.core.domain.YnabTransaction(id = id, accountId = accountId, amount = amount)

fun YnabTransaction.Body.toFinanceDomain() =
    noodle.finance.core.domain.YnabTransaction.Body(
        transactionIds = transactionIds,
        transaction = transaction?.toFinanceDomain(),
        transactions = transactions?.map { it.toFinanceDomain() },
    )

fun YnabTransaction.Data.toFinanceDomain() =
    noodle.finance.core.domain.YnabTransaction.Data(data = data.toFinanceDomain())

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
