package noodle.finance.core.domain

data class YnabBudget(
    val id: String,
    val name: String,
) {
    data class Body(val budgets: List<YnabBudget>)

    data class Data(val data: Body)
}
