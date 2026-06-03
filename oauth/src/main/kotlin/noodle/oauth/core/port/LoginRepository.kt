package noodle.oauth.core.port

import noodle.oauth.core.domain.Login

public interface LoginRepository {
    public suspend fun putLogin(login: Login)
}
