package noodle.finance.budget.bridge

data class PubsubNotification(val message: PubsubMessage, val subscription: String)
