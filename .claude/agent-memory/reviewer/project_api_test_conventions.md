---
name: api-test-conventions
description: Gold-standard structure for *ApiTests in shared api modules — what a compliant serialization test looks like
metadata:
  type: project
---

The two reference `*ApiTests` (style baseline, do not "fix" them): `ynab-api/.../YnabApiTests.kt`, `gmail-api/.../GmailApiTests.kt`.

Compliant shared-api test shape:
- Class annotated `@DisabledInNativeImage` + `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`.
- `MockEngine` with two `when (method to encodedPath)` blocks: first builds the JSON `text`, second selects `respond/respondOk/respondError`.
- Mock clients must NOT assert HTTP status; deserialize the body via `.body<T>()` and assert one field per line so missing asserts are visually obvious.
- Every field of the deserialized model gets an `assertEquals`/`assertNull` (use `assertNull` for absent-by-design fields — makes regressions visible). `YnabAuthApiTests` is the strongest example.
- Expression-body `runBlocking { }` for test methods.

**Why:** branch `test/api-serialization` standardized assertion completeness across these files.
**How to apply:** when reviewing api tests, check field-by-field coverage against the model class, flag trailing-comma JSON (lenient parser hides it), prefer `json.decodeFromString<T>(raw)` over reflective `serializer(typeOf<T>())`.

Dependency hygiene: a test that references another module's model (e.g. `ynab-auth-api` -> `oauth2-api` `OAuth2TokenResponse`) must use `testImplementation(project(...))`, never a main `implementation`. `@Serializable` model deserialization requires the `kotlin.serialization` compiler plugin in that module's build file.
