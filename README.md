# gmail-ynab-bridge

A serverless bridge that monitors Gmail for financial notification emails, parses them into YNAB transactions, and delivers status updates to a Telegram bot. Deployed as GraalVM native AWS Lambda functions backed by DynamoDB, with OAuth2 support for both Google and YNAB.

---

## Architecture

The project follows hexagonal (ports-and-adapters) architecture across six layers:

- **Core** — domain entities, use-case services, and output port interfaces; no outbound dependencies
- **Drivers** — transport/framework setup and base abstractions (Ktor `HttpClient` config, DynamoDB base classes) — knows a transport technology, not any application domain
- **Integration clients** — Ktor HTTP clients for external APIs; no application domain knowledge
- **Application adapters** — implement core output ports by delegating to integration clients
- **Persistence** — DynamoDB implementations of core repository ports
- **Bootstrap** — AWS Lambda handlers; the only layer that imports peers to wire the full dependency graph

Each application (`oauth`, `gmailsync`, `ynabsync`, `telegramchat`) is fully isolated at the core layer and shares nothing except common libraries. Konsist architecture tests enforce these layer boundaries.

---

## Modules

### Core

| Module | Package | Responsibility |
|---|---|---|
| `oauth` | `noodle.oauth.core` | OAuth2 token lifecycle for Google and YNAB; domain: `User`, `Login`, `Token`; services: `TokenService`, `AuthorizeService` |
| `gmailsync` | `noodle.gmailsync.core` | Gmail Pub/Sub notification processing; domain: `Bridge`, `Mailbox`, `Outbox`; service: `GmailPubsubService` |
| `ynabsync` | `noodle.ynabsync.core` | Email-to-YNAB transaction mapping; domain: `YnabTransaction`, `TransactionMatcher`, `Bridge`, `Configuration`; service: `YnabEmailService` |
| `telegramchat` | `noodle.telegramchat.core` | Telegram bot command handling for OAuth flows and Gmail watch setup; domain: `User`, `Login`, `StateToken`, `GmailWatch`; service: `TelegramBotService` |

### Integration clients

Ktor HTTP wrappers for external APIs. Each knows one external service and nothing about application domain types.

| Module | Package | Responsibility |
|---|---|---|
| `gmail-api` | `noodle.gmail.infrastructure.api` | Gmail REST API client — history, messages, labels, watch, Pub/Sub models |
| `ynab-api` | `noodle.ynab.infrastructure.api` | YNAB API client — budgets, accounts, transactions |
| `ynab-auth-api` | `noodle.ynab.auth.infrastructure.api` | YNAB OAuth2 token endpoint client |
| `google-auth-api` | `noodle.google.auth.infrastructure.api` | Google OAuth2 token endpoint client |
| `telegram-api` | `noodle.telegram.infrastructure.api` | Telegram Bot API client — `sendMessage`, `sendChatAction`, webhook models |
| `oauth2-api` | `noodle.oauth2.infrastructure.api` | Provider-agnostic OAuth2 — stateless OIDC discovery (`OidcApi`, `OidcDiscoveryDocument`) and the `OAuth2TokenResponse` model; depends only on the `ktor` driver |

### Application adapters

Translate core output ports to integration clients. Each adapter module depends on one core and one or more integration clients.

| Module | Package | Responsibility |
|---|---|---|
| `gmailsync-api` | `noodle.gmailsync.infrastructure.api` | Implements `gmailsync` `GmailClient`/`Factory` and `OAuth2Client` ports via `gmail-api` + `google-auth-api` |
| `ynabsync-api` | `noodle.ynabsync.infrastructure.api` | Implements `ynabsync` `GmailClient`/`Factory` and `YnabClient`/`Factory` ports via `gmail-api` + `ynab-api` |
| `telegramchat-api` | `noodle.telegramchat.infrastructure.api` | Implements `telegramchat` `TelegramBotClient` and `GmailClient`/`Factory` ports via `telegram-api` + `gmail-api` |
| `oauth-google-api` | `noodle.oauth.infrastructure.api.google` | Implements `oauth` `OAuth2TokenProvider` and `LoginIdProvider` ports via `google-auth-api` + `oauth2-api` |
| `oauth-ynab-api` | `noodle.oauth.infrastructure.api.ynab` | Implements `oauth` `OAuth2TokenProvider` and `LoginIdProvider` ports via `ynab-auth-api` |

### Persistence

DynamoDB implementations of core repository ports.

| Module | Package | Responsibility |
|---|---|---|
| `oauth-persistence` | `noodle.oauth.infrastructure.persistence` | `DynamoDbUserRepository`, `DynamoDbLoginRepository`, `DynamoDbTokenRepository` |
| `gmailsync-persistence` | `noodle.gmailsync.infrastructure.persistence` | `DynamoDbBridgeRepository`, `DynamoDbMailboxRepository`, `DynamoDbOutboxRepository` |
| `ynabsync-persistence` | `noodle.ynabsync.infrastructure.persistence` | `DynamoDbBridgeRepository`, `DynamoDbMatcherRepository`, `DynamoDbOutboxRepository` |
| `telegramchat-persistence` | `noodle.telegramchat.infrastructure.persistence` | `DynamoDbUserRepository`, `DynamoDbLoginRepository`, `DynamoDbTokenRepository`, `DynamoDbMailboxRepository` |

### Common libraries

Shared infrastructure utilities. No application domain knowledge; extracted when two or more modules need the same transport or tooling.

| Type | Module | Package | Responsibility |
|---|---|---|---|
| Driver | `ktor` | `noodle.ktor` | Shared Ktor `HttpClient` config fragments (logging, JSON serialization, Authorization-header sanitization) routed through all API clients |
| Driver | `dynamodb` | `noodle.dynamodb` | `DynamoDbRepository` and `DynamoDbSortRepository` base abstractions (CRUD + range-key queries) |
| Integration | `bitwarden-api` | `noodle.bitwarden.infrastructure.api` | Fetches encrypted credentials from AWS Secrets Manager via the Bitwarden SDK, wrapping the payload in a `BitwardenSecret` value class |

### Bootstrap

AWS Lambda handlers compiled to GraalVM native images. Each bootstrap is the composition root for one deployment: it instantiates all adapters, wires output ports to their implementations, and handles the Lambda event.

| Module | Package | Lambda trigger | Responsibility |
|---|---|---|---|
| `oauth-google-bootstrap` | `noodle.oauth.bootstrap.google` | API Gateway | Google OAuth2 callback — wires `AuthorizeService` + Google adapters |
| `oauth-ynab-bootstrap` | `noodle.oauth.bootstrap.ynab` | API Gateway | YNAB OAuth2 callback — wires `AuthorizeService` + YNAB adapters |
| `gmailsync-bootstrap` | `noodle.gmailsync.bootstrap` | API Gateway (Pub/Sub push) | Gmail Pub/Sub notification handler — wires `GmailPubsubService` + all adapters |
| `ynabsync-bootstrap` | `noodle.ynabsync.bootstrap` | DynamoDB Streams | YNAB email sync handler — wires `YnabEmailService` + all adapters |
| `telegramchat-bootstrap` | `noodle.telegramchat.bootstrap` | API Gateway (webhook) | Telegram bot webhook handler — wires `TelegramBotService` + all adapters |

---

## Build

```
./gradlew build                           # compile and assemble all modules
./gradlew check                           # compile, test, and lint all modules
./gradlew kF                              # format all Kotlin source files (ktlint)
./gradlew :architecture-test:test         # run Konsist hexagonal boundary tests
```

Convention plugins and a shared version catalog live in `buildSrc/` and `gradle/libs.versions.toml` respectively. Build cache and configuration cache are enabled in `gradle.properties`.

The `architecture-test` module (package `noodle.architecture`) uses Konsist to enforce hexagonal layer boundaries: core must not import infrastructure or bootstrap; domain must not import services; nothing may import bootstrap; no cross-application infrastructure coupling.

Kotlin **Strict explicitApi** mode is enabled repo-wide via the shared `kotlin-jvm` convention plugin (`buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`): every public declaration requires an explicit visibility modifier.

---

## Tech stack

| Technology | Role |
|---|---|
| Kotlin 2.3 / Java 21 | Primary language and toolchain |
| Ktor 3 | HTTP client for all external API integrations |
| AWS Lambda | Serverless compute for all five entry points |
| AWS DynamoDB | State, repository, and outbox persistence |
| GraalVM Native Image | Ahead-of-time compiled Lambda functions for fast cold starts |
| Bitwarden Secrets SDK | Encrypted credential retrieval at runtime |
| kotlinx.coroutines | Async concurrency across all service layers |
