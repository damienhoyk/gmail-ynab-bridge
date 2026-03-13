/**
 * Core Domain Entity representing a user in the system.
 */
data class User(val username: String, val name: String)

/**
 * Outbound Port interface defined by the core domain for user persistence.
 */
interface UserRepository {
    fun save(user: User)
}

/**
 * Outbound Adapter implementing the [UserRepository] port for a PostgreSQL database.
 */
class PostgresUserRepository : UserRepository {
    override fun save(user: User) {
        println("Saving ${user.name} to PostgreSQL database.")
    }
}

/**
 * Inbound Port / Use Case that implements the core registration logic.
 */
class RegisterUserService(private val userRepository: UserRepository) {
    fun execute(command: RegisterUserCommand) {
        val newUser = User(username = command.username, name = command.name)
        userRepository.save(newUser)
    }
}

/**
 * Infrastructure model (DTO) representing the raw data from an incoming request.
 */
@Serializable
data class RegisterUserRequest(val userId: Int, val name: String)

/**
 * Domain command representing the validated intent to register a user.
 */
data class RegisterUserCommand(val userId: Int, val name: String) {
    val username: String 
        get() = "$userId@domain"
}

/**
 * Inbound Adapter that handles host-specific triggers (e.g., HTTP) and maps them to domain commands.
 */
class RegisterUserHandler(private val registerUserService: RegisterUserService) {
    fun handle(body: String) {
        val request = Json.decodeFromString<RegisterUserRequest>(body)
        val command = RegisterUserCommand(userId = request.userId, name = request.name) 
        registerUserService.execute(command)
    }
}