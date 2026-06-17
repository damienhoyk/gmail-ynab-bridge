package noodle.ynabsync.core.port

public interface LoginRepository {
    public suspend fun resolve(email: String): String?
}
