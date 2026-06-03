package noodle.ynab.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class YnabBudget(
    public val id: String,
    public val name: String,
) {
    @Serializable
    public data class Body(
        public val budgets: List<YnabBudget>,
    )

    @Serializable
    public data class Data(
        public val data: Body,
    )
}
