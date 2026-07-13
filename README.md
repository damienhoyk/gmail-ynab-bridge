# gmail-ynab-bridge

A serverless bridge that monitors Gmail for financial notification emails, parses them into YNAB transactions, and delivers status updates to a Telegram bot. Deployed as GraalVM native AWS Lambda functions backed by DynamoDB. OAuth2 tokens are proactively refreshed on a schedule; sync and chat apps consume fresh tokens from the token repository without managing OAuth flows themselves.

---

## Architecture

The project follows hexagonal (ports-and-adapters) architecture across six layers:

- **Core** — domain entities, use-case services, and output port interfaces; no outbound dependencies
- **Drivers** — transport/framework setup and base abstractions (Ktor `HttpClient` config, DynamoDB base classes) — knows a transport technology, not any application domain
- **Integration clients** — Ktor HTTP clients for external APIs; no application domain knowledge
- **Application adapters** — implement core output ports by delegating to integration clients
- **Persistence** — DynamoDB implementations of core repository ports
- **Bootstrap** — AWS Lambda handlers; the only layer that imports peers to wire the full dependency graph

Each application (`oauth`, `gmailsync`, `ynabsync`, `telegramchat`, `tokenrefresher`) is fully isolated at the core layer and shares nothing except common libraries. Konsist architecture tests enforce these layer boundaries.

---

## Modules

### Core

| Module | Package | Responsibility |
|---|---|---|
| `oauth` | `noodle.oauth.core` | OAuth2 token lifecycle for Google and YNAB; domain: `User`, `Login`, `Token`; services: `TokenService`, `AuthorizeService` |
| `gmailsync` | `noodle.gmailsync.core` | Gmail Pub/Sub notification processing; domain: `Bridge`, `Mailbox`, `Outbox`; service: `GmailPubsubService` |
| `ynabsync` | `noodle.ynabsync.core` | Email-to-YNAB transaction mapping; domain: `YnabTransaction`, `TransactionMatcher`, `BankAccount`, `Configuration`; service: `YnabEmailService` |
| `telegramchat` | `noodle.telegramchat.core` | Telegram bot command handling for OAuth flows and Gmail watch setup; domain: `User`, `Login`, `StateToken`, `GmailWatch`; service: `TelegramBotService` |
| `tokenrefresher` | `noodle.tokenrefresher.core` | Proactive OAuth2 token refresh; domain: `RefreshedToken`; service: `TokenRefreshService` |

### Resource identifiers (ynabsync)

`ynabsync` locates YNAB resources via scheme-relative URI identifiers of the form `//authority/path`. The persistence adapter (`DynamoDbAccountRepository`) validates each sort key with `java.net.URI`: it must have no scheme, host `app.ynab.com` (matched case-insensitively, RFC 3986), non-blank userInfo, and a `/budget/<id>/account/<id>` path; malformed sort keys are logged and skipped without throwing.

| Identifier | Form | Example | Uses |
|---|---|---|---|
| Bank account | `//<sub>@gmail.com/account/<number>` | `//abc-123@gmail.com/account/9062` | `bank-account.partition` (partition key) |
| YNAB account | `//<userId>@app.ynab.com/budget/<budgetId>/account/<accountId>` | `//abc-123@app.ynab.com/budget/def-456/account/ghi-789` | `bank-account.sort` (sort key; parsed in `DynamoDbAccountRepository`) |
| User | `//<userId>@app.ynab.com` | `//abc-123@app.ynab.com` | `bridge.destination` (outbox routing) |

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
| `gmailsync-api` | `noodle.gmailsync.infrastructure.api` | Implements `gmailsync` `GmailClient`/`Factory` ports via `gmail-api`; uses read-only bearer token from `ktor` driver's `AuthConfig.bearer()` |
| `ynabsync-api` | `noodle.ynabsync.infrastructure.api` | Implements `ynabsync` `YnabClient`/`Factory` ports via `ynab-api`; uses read-only bearer token from `ktor` driver's `AuthConfig.bearer()` |
| `telegramchat-api` | `noodle.telegramchat.infrastructure.api` | Implements `telegramchat` `TelegramBotClient` and `GmailClient`/`Factory` ports via `telegram-api` + `gmail-api`; uses read-only bearer tokens from `ktor` driver's `AuthConfig.bearer()` |
| `tokenrefresher-google-api` | `noodle.tokenrefresher.infrastructure.api.google` | Implements `tokenrefresher` `TokenProvider` port via `google-auth-api` + `oauth2-api` |
| `tokenrefresher-ynab-api` | `noodle.tokenrefresher.infrastructure.api.ynab` | Implements `tokenrefresher` `TokenProvider` port via `ynab-auth-api` |
| `oauth-api` | `noodle.oauth.infrastructure.api` | Retains the lazy `AuthConfig.bearer(service, loginId)` helper that refreshes OAuth2 tokens via `TokenService` on 401. Now unused after sync/chat apps moved to the `ktor` driver's static `bearer(accessToken)`; retained pending oauth retirement. |
| `oauth-google-api` | `noodle.oauth.infrastructure.api.google` | Implements `oauth` `OAuth2TokenProvider` and `LoginIdProvider` ports via `google-auth-api` + `oauth2-api` |
| `oauth-ynab-api` | `noodle.oauth.infrastructure.api.ynab` | Implements `oauth` `OAuth2TokenProvider` and `LoginIdProvider` ports via `ynab-auth-api` |

### Persistence

DynamoDB implementations of core repository ports.

| Module | Package | Responsibility |
|---|---|---|
| `oauth-persistence` | `noodle.oauth.infrastructure.persistence` | `DynamoDbUserRepository`, `DynamoDbLoginRepository`, `DynamoDbTokenRepository` |
| `gmailsync-persistence` | `noodle.gmailsync.infrastructure.persistence` | `DynamoDbBridgeRepository`, `DynamoDbMailboxRepository`, `DynamoDbOutboxRepository` |
| `ynabsync-persistence` | `noodle.ynabsync.infrastructure.persistence` | `DynamoDbAccountRepository`, `DynamoDbMatcherRepository`, `DynamoDbOutboxRepository` |
| `telegramchat-persistence` | `noodle.telegramchat.infrastructure.persistence` | `DynamoDbUserRepository`, `DynamoDbLoginRepository`, `DynamoDbTokenRepository`, `DynamoDbMailboxRepository` |
| `tokenrefresher-persistence` | `noodle.tokenrefresher.infrastructure.persistence` | `DynamoDbTokenRepository` — read and update OAuth tokens in the shared `token` table; paginated scan with bounded concurrency ≤5 |

### Common libraries

Shared infrastructure utilities. No application domain knowledge; extracted when two or more modules need the same transport or tooling.

| Type | Module | Package | Responsibility |
|---|---|---|---|
| Driver | `ktor` | `noodle.ktor` | Shared Ktor `HttpClient` config fragments (logging, JSON serialization, Authorization-header sanitization); generic `bearer(accessToken)` helper for attaching OAuth2 tokens to outbound requests |
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
| `tokenrefresher-bootstrap` | `noodle.tokenrefresher.bootstrap` | EventBridge Scheduler (`rate(15 minutes)`) | Proactive token refresher — scans `token` table, refreshes each token's refresh-token via Google/YNAB APIs, updates rotated tokens; bounded concurrency ≤5, reserved concurrency = 1 |

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
