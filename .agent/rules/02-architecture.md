---
trigger: always_on
---

# Architecture Guidelines

- Isolate the core domain logic from external frameworks, databases, and user interfaces.
- Define strictly typed interfaces (Ports) for all external communication.
- Implement Adapters for specific technologies (e.g., PostgreSQL adapter, REST API adapter) that plug into the Ports.
- Enforce the dependency rule where external layers depend on inner layers, never the reverse.

## Standard Port and Adapter Implementation
For a comprehensive example of Port and Adapter implementation in Kotlin, refer to [hexagonal-kotlin.kt](../examples/architecture/hexagonal-kotlin.kt).