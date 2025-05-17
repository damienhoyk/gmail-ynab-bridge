package noodle.home.security

interface CredentialsProvider {
    val clientId: String?
    val clientSecret: String?
    fun load()
}