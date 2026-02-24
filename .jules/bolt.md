## 2025-05-15 - [Anti-pattern: In-line Regex Compilation]
**Learning:** Utility functions in `google-gmail/Extensions.kt` were compiling Regex objects on every call. In a high-volume processing environment (like a Gmail bridge), this can significantly degrade performance during text normalization of large numbers of message parts.
**Action:** Always pre-compile Regex objects as top-level private properties or companion object properties in Kotlin utility files.

## 2025-05-15 - [Build Failure: Missing GitHub Credentials]
**Learning:** The build system was hard-casting missing properties (`github.user`, `github.key`) to non-null Strings, causing a crash during configuration when credentials for the Bitwarden Maven repository were missing. Defaulting to empty strings also proved problematic as it could lead to invalid credential attempts.
**Action:** Use conditional configuration (`if (hasProperty(...))`) to only apply credentials if they are provided, allowing local builds/tests to proceed safely if the private dependencies are not required for the current task.
