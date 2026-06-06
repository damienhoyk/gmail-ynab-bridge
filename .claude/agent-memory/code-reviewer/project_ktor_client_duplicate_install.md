---
name: ktor-client-duplicate-install-merges
description: ktor CLIENT install() merges duplicate plugin configs (no throw); only the SERVER throws DuplicateApplicationFeature
metadata:
  type: project
---

On the ktor **client** (`HttpClientConfig.install(plugin, configure)`), installing the same plugin key twice does NOT throw — it merges by chaining the config blocks: previous block runs first, then the new one (`previousConfigBlock?.invoke(this); configure()`). So `defaultJson()` / `defaultLogging()` from a shared fragment plus a caller's own `install(...)` block is safe — both run, last-write-wins on overlapping settings.

This is the opposite of the ktor **server**, where a second `install(ContentNegotiation)` throws `DuplicateApplicationFeatureException` (same-key conflict). Web searches about "ktor duplicate install" surface the server behavior — do not apply it to client code.

**Why:** the GoogleOAuth2Api/YnabApi/OidcDiscoveryApi clients all take an optional `block: HttpClientConfig<*>.() -> Unit = {}` after their fragment installs; reviewers worry a caller's block double-installs ContentNegotiation. It can't break.
**How to apply:** when reviewing the `<infra>-api` HttpClient builders (which install shared fragments then run `block()`), do not flag potential double-install of ContentNegotiation/Logging as a bug. Engines (`Java.create()`, `MockEngine`) provide transport only and never install client plugins, so the bootstrap construction `GoogleOAuth2Api(HttpClient(Java.create()))` with no block installs ContentNegotiation exactly once. ktor `installOrReplace()` exists for replace-not-merge semantics but is not used here.
