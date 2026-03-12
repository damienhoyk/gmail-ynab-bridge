package noodle.finance.domain

data class Bridge(val source: String, val destination: String, val accounts: Map<String, String>?)
