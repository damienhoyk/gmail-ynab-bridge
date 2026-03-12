package noodle.chat.port.out

import noodle.chat.domain.Login

interface LoginRepository {
    suspend fun putLogin(login: Login)

    suspend fun getLogin(id: String): Login?
}
