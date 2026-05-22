package noodle.ynabsync.core.domain

data class YnabAccount(
    val id: String,
    val name: String,
) {
    data class Body(
        val accounts: List<YnabAccount>,
    )

    data class Data(
        val data: Body,
    )
}
