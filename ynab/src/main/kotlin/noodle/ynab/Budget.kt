package noodle.ynab

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: String,
    val name: String
) {

    @Serializable
    data class Body(val budgets: List<Budget>)


    @Serializable
    data class Data(val data: Body)

}
