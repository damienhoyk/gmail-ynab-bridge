package noodle.security.port.out

import noodle.security.domain.Login

interface LoginRepository {
    suspend fun putLogin(login: Login)
}
