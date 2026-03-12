package noodle.security.domain

data class AuthorizeCommand(val code: String?, val state: String?)
