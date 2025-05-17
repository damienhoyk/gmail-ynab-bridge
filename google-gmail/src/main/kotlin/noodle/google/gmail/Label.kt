package noodle.google.gmail

import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val id: String,
    val name: String
) {

    @Serializable
    data class List(val labels: kotlin.collections.List<Label>? = null)

}
