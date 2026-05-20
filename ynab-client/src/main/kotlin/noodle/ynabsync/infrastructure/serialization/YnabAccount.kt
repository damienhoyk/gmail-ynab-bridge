package noodle.finance.infrastructure.serialization

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccount(
    val id: String,
    val name: String,
) {
    @Serializable
    data class Body(val accounts: List<YnabAccount>)

    @Serializable
    data class Data(val data: Body)
}
