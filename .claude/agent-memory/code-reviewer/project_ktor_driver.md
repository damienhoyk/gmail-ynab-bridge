---
name: ktor-driver-fragments
description: ktor driver module holds shared HttpClientConfig DSL fragments (defaultLogging/defaultJson); 6 HTTP clients consume them
type: project
---

The `ktor` driver module (`noodle.ktor` package) holds reusable `HttpClientConfig<*>` extension fragments instead of a factory:
- `defaultLogging(level = INFO)` — installs Logging with Logger.DEFAULT + `sanitizeHeader { it == Authorization }` (security: prevents token leakage in logs)
- `defaultJson(json = DefaultJson)` — installs ContentNegotiation json
- `DefaultJson` = `Json { ignoreUnknownKeys = true }`

As of 2026-06-05 the file is named `HttpClientConfig.kt` (package `noodle.ktor`) and holds `DefaultJson` + `defaultLogging`/`defaultJson`. (Earlier it was `HttpClientFactory.kt`; the rename happened.)

Consuming clients, each composing fragments inside `httpClient.config { ... block() }`:
- OidcDiscoveryApi + OAuth2TokenApi (oauth2-api), GoogleOAuth2Api (google-auth-api), YnabApi (ynab-api), GmailApi (gmail-api): logging + json
- TelegramBotApi (telegram-api): `defaultLogging(LogLevel.BODY)` only, NO json
- ynab-auth-api no longer has an HTTP client — it is now just `object YnabAuthApi { const val TOKEN_ENDPOINT }` (as of 2026-06-05 refactor).

**Why:** de-duplicate per-client install blocks behind a transport-only driver.
**How to apply:** the `ktor` build.gradle.kts must stay transport-only (platform + bundles.ktor.client) — no app/integration project deps. As of 2026-06-05 the OIDC discovery client is `OidcDiscoveryApi` and lives in `oauth2-api` (not `oauth-api`). When reviewing client config changes, watch for accidental loss of `sanitizeHeader`, log-level drift, or unintended ContentNegotiation on TelegramBotApi.
