package noodle.security.core.domain

data class AuthorizeCommand(val code: String?, val state: String?)
