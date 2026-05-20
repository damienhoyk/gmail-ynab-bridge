package noodle.security.core.port

import noodle.security.core.domain.User

interface UserRepository {
    suspend fun putUser(user: User)
}
