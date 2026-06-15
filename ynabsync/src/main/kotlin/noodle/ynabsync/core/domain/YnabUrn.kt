package noodle.ynabsync.core.domain

public data class YnabUrn(
    val userId: String,
    val budgetId: String,
    val accountId: String,
) {
    public companion object {
        public fun parse(input: String): YnabUrn? {
            val parts = input.split(":")
            if (parts.size != 7) return null
            if (parts[0] != "urn") return null
            if (parts[1] != "app.ynab.com") return null
            if (parts[3] != "budget") return null
            if (parts[5] != "account") return null

            val userId = parts[2]
            val budgetId = parts[4]
            val accountId = parts[6]

            if (userId.isBlank() || budgetId.isBlank() || accountId.isBlank()) return null

            return YnabUrn(userId, budgetId, accountId)
        }
    }
}
