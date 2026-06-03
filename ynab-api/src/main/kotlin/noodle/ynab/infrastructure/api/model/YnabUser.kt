package noodle.ynab.infrastructure.api.model

import kotlinx.serialization.Serializable

@Serializable
public data class YnabUser(
    public val id: String,
) {
    @Serializable
    public data class Body(
        public val user: YnabUser,
    )

    @Serializable
    public data class Data(
        public val data: Body,
    )
}
