---
trigger: always_on
---

# Architecture Guidelines

- Isolate the core domain logic from external frameworks, databases, and user interfaces.
- Define strictly typed interfaces (Ports) for all external communication.
- Implement Adapters for specific technologies (e.g., PostgreSQL adapter, REST API adapter) that plug into the Ports.
- Enforce the dependency rule where external layers depend on inner layers, never the reverse.

## Standard Port and Adapter Implementation
```kotlin
// 1. Core Domain Entity
data class User(val id: Int, val name: String)

// 2. Port (Interface defined by the core domain)
interface UserRepository {
    fun save(user: User)
}

// 3. Adapter (Implementation of the Port for a specific technology)
class PostgresUserRepository : UserRepository {
    override fun save(user: User) {
        println("Saving ${user.name} to PostgreSQL database.")
    }
}

// 4. Use Case (Core logic utilizing the Port)
class RegisterUserUseCase(private val userRepository: UserRepository) {
    fun execute(userId: Int, name: String) {
        val newUser = User(id = userId, name = name)
        userRepository.save(newUser)
    }
}