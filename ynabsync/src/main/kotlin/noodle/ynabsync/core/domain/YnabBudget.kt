package noodle.ynabsync.core.domain

public data class YnabBudget(
    public val id: String,
    public val name: String,
) {
    public data class Body(
        public val budgets: List<YnabBudget>,
    )

    public data class Data(
        public val data: Body,
    )
}
