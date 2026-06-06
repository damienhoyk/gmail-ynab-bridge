---
name: api-test-patterns
description: Two established test patterns for *Api integration modules (MockEngine behaviour vs serialization) and their native-image annotation rules
metadata:
  type: project
---

Each `*-api` integration module has up to two test classes with distinct native-image rules:

- **MockEngine behaviour test** (e.g. `GmailApiTests`, `TelegramBotApiTests`, `YnabApiTests`, `YnabAuthApiTests`, `GoogleOAuth2ApiTests`): spins up `HttpClient(MockEngine{...})`, routes on `it.method to it.url.encodedPath`, asserts request routing + response decoding. MUST be `@DisabledInNativeImage` (ktor MockEngine is not native-reachable). YnabApi/Gmail also add `@TestInstance(PER_CLASS)` because they hold randomized `val` fixtures used in mock responses. Routes are matched against the production class's base url + path segment (e.g. YnabApi base `https://api.ynab.com/v1/` + `get("user")` -> `/v1/user`).
- **Serialization test** (e.g. `oauth2-api/TokenSerializationTests`, `ynab-api/.../model/YnabSerializationTests`): pure kotlinx.serialization `json.decodeFromString(serializer(typeOf<T>()), raw)`, no HTTP. MUST stay ENABLED in native image — it is what validates serializer reachability metadata for `nativeCompile`. Lives in the `model` subpackage. Module needs `id("kotlin-native-test")` + a `graalvmNative` test binary block pointing at `META-INF/native-image/<group>/<module>`.

**Why:** the serialization test is the native-image canary; disabling it would silently drop serializer reachability coverage. The MockEngine test must be disabled because MockEngine isn't native-reachable.

**How to apply:** when reviewing a new `*Api` test, confirm the MockEngine one is `@DisabledInNativeImage` and the serialization one is NOT, and that the module's build.gradle has `kotlin-native-test` + the native-image config dir. `testImplementation("io.ktor:ktor-client-mock")` is intentionally versionless (ktor BOM platform supplies the version) — consistent across all api modules. See [[project-oauth-token-api-layout]] and [[ktor-driver-fragments]].
