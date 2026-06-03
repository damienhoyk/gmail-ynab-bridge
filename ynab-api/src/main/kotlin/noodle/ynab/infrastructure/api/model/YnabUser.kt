package noodle.ynab.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
data class YnabUser(
    val id: String,
) {
    @Serializable
    data class Body(
        val user: YnabUser,
    )

    @Serializable
    data class Data(
        val data: Body,
    )
}
