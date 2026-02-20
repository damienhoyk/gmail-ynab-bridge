## 2025-05-15 - [Anti-pattern: In-line Regex Compilation]
**Learning:** Utility functions in `google-gmail/Extensions.kt` were compiling Regex objects on every call. In a high-volume processing environment (like a Gmail bridge), this can significantly degrade performance during text normalization of large numbers of message parts.
**Action:** Always pre-compile Regex objects as top-level private properties or companion object properties in Kotlin utility files.

## 2025-05-15 - [Build Failure: Missing GitHub Credentials]
**Learning:** The build system was hard-casting missing properties (`github.user`, `github.key`) to non-null Strings, causing a crash during configuration when credentials for the Bitwarden Maven repository were missing.
**Action:** Use safe casting (`as? String ?: ""`) for optional build properties to allow local builds/tests to proceed even if some private dependencies cannot be resolved.
