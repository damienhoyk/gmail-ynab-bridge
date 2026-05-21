package noodle.ynabsync.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class YnabBudget(
    val id: String,
    val name: String,
) {
    @Serializable
    data class Body(val budgets: List<YnabBudget>)

    @Serializable
    data class Data(val data: Body)
}
