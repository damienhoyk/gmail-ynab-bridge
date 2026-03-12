package noodle.security.port.out

import noodle.security.domain.User

interface UserRepository {
    suspend fun putUser(user: User)
}
