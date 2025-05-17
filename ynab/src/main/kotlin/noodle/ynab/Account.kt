package noodle.ynab

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String
) {

    @Serializable
    data class Body(val accounts: List<Account>)


    @Serializable
    data class Data(val data: Body)

}
