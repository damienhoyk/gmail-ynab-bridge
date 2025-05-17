package noodle.finance.budget.bridge

import kotlinx.serialization.Serializable

@Serializable
data class PubsubMessage(val data: String, val messageId: String, val publishTime: String)