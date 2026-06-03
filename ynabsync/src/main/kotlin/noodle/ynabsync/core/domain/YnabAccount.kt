package noodle.ynabsync.core.domain

public data class YnabAccount(
    public val id: String,
    public val name: String,
) {
    public data class Body(
        public val accounts: List<YnabAccount>,
    )

    public data class Data(
        public val data: Body,
    )
}
