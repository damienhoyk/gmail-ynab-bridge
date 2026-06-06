---
name: oauth-token-clients
description: Where the YnabAuthApi / GoogleOAuth2Api token clients actually live and their config shape
metadata:
  type: project
---

OAuth2 token-endpoint clients (full submitForm clients, NOT reduced to consts):
- `YnabAuthApi` — `ynab-auth-api`, package `noodle.ynab.auth.infrastructure.api`. base url `https://app.ynab.com/oauth/`, posts to `token`. `requestToken(parameters)` -> HttpResponse decoded as oauth2-api `OAuth2TokenResponse`.
- `GoogleOAuth2Api` — `google-auth-api`, package `noodle.google.auth.infrastructure.api`. base url `https://oauth2.googleapis.com/`, posts to `tokeninfo`. `requestTokenInfo(parameters)` -> decoded as local model `TokenInfoResponse`.

Client config shape (matches reference `oauth2-api/OidcApi.kt`): `httpClient.config { defaultLogging(); defaultJson(); defaultRequest { url(urlString) }; block() }`. submitForm sends `application/x-www-form-urlencoded`; request content type must NOT be set to JSON. Response JSON decoding is handled by defaultJson()/ContentNegotiation via Accept header + response Content-Type, independent of request body content type.

**Why:** supersedes the stale claim in code-reviewer/project_oauth_token_api_layout.md that YnabAuthApi was reduced to a `const TOKEN_ENDPOINT`. As of branch chore/drop-json-content-type (2026-06) both are full clients.
**How to apply:** when reviewing these clients, the `defaultRequest { url() }` block is base-URL setup and legitimate; only a request-side `contentType(Application.Json)` would be the anti-pattern.
