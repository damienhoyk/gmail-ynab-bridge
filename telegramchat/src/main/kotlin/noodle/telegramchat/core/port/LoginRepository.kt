package noodle.telegramchat.core.port

import noodle.telegramchat.core.domain.Login

public interface LoginRepository {
    public suspend fun putLogin(login: Login)

    public suspend fun getUser(id: String): String?
}
