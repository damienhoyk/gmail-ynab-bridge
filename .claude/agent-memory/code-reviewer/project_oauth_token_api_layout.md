---
name: project-oauth-token-api-layout
description: Where OAuth token API classes actually live across modules (non-obvious; OidcApi is not in oauth-google-api)
metadata:
  type: project
---

OAuth token-acquisition classes are split across modules in a non-obvious way:

- `oauth2-api` (shared integration, package `noodle.oauth2.infrastructure.api`, depends only on `:ktor`): provider-agnostic OAuth2 token protocol. Owns wire DTO `OAuth2TokenResponse` (@SerialName access_token/id_token/refresh_token/expires_in/error), `OAuth2TokenApi(httpClient, tokenEndpoint: String)` (plain string now, not suspend lambda — installs defaultLogging+defaultJson, exposes `postToken(): HttpResponse`), `OidcDiscoveryApi(httpClient, discoveryUrl)` (stateless: only `getDiscoveryDocument(): OidcDiscoveryDocument`, no memoization), and `OidcDiscoveryDocument(tokenEndpoint: String?)` with @SerialName("token_endpoint"). MUST NOT import any `noodle.*.core.*` or other integrations.
  - NOTE: `oauth2-api` ALSO still defines `OAuth2TokenRequest`+`toForm()` — but this is DEAD CODE. The real mapping uses the core-domain `OAuth2TokenRequest` + its `toForm()` in oauth-api Extensions.kt. The oauth2-api copy has no callers (verify before relying on it).
- `OAuth2Client`: `oauth-api`, package `noodle.oauth.infrastructure.api` (renamed from old `OAuth2TokenClient`/`OAuth2OidcClient`, both deleted). Implements `noodle.oauth.core.port.OAuth2TokenProvider`, wraps `OAuth2TokenApi`, the ONLY place mapping core domain types ↔ oauth2-api types. `.domain()` ext (in OAuth2Client.kt) maps OAuth2TokenResponse→core TokenResponse, all 5 fields 1:1.
  - Discovery fetch-once now lives at the composition seam: each bootstrap calls `OidcDiscoveryApi(...).getDiscoveryDocument()` ONCE inside `initScope.async{}`, passes `doc.tokenEndpoint ?: throw IllegalStateException(...)` into `OAuth2TokenApi`. ynabsync-bootstrap has BOTH a Google (discovery) path and a YNAB (YnabAuthApi.TOKEN_ENDPOINT hardcoded) path.
- `YnabAuthApi`: `ynab-auth-api`, package `noodle.ynab.auth.infrastructure.api`. Reduced to just `const val TOKEN_ENDPOINT = "https://app.ynab.com/oauth/token"` — module's build deps (ktor, oauth2-api, serialization plugin) are now stale/unused.
- `GoogleOAuth2Api` (tokeninfo): `google-auth-api`, package `noodle.google.auth.infrastructure.api` (depends only on :ktor).
- `KtorGoogleLoginIdProvider`: `oauth-google-api`, package `noodle.oauth.infrastructure.api.google`.
- `KtorYnabLoginIdProvider`: `oauth-ynab-api`, package `noodle.oauth.infrastructure.api.ynab` — imports `noodle.oauth.infrastructure.api.bearer` from :oauth-api, so oauth-ynab-api legitimately keeps its :oauth-api dependency.

**Why:** After the abstract KtorOAuth2TokenProvider was removed in favor of composed OAuth2TokenClient(post::method-ref), then the oauth2-api extraction, the token-acquisition pieces fanned out. Reviewers chasing a class by its declared package will not find it under the expected module dir.

**How to apply:** When locating an OAuth token class, search by package not module dir; oauth-google-api/oauth-ynab-api hold only the LoginIdProvider adapters (under .google/.ynab subpackages), not the token APIs. Note the wire-DTO test `TokenSerializationTests.kt` lives in `oauth-api` test src (package noodle.oauth.infrastructure.api) even though the DTO it tests now lives in oauth2-api — test was not relocated with the DTO.
