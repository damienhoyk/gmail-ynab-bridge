---
name: dependency-guard
description: Use this when the user wants to add a new library or modify build.gradle/build.gradle.kts.
---
# Dependency Guard

When the user wants to add a dependency:
* **Check Compatibility**: Ensure the library works with the current Java/Kotlin version.
* **Preference**: Prioritize official JetBrains/Google libraries over third-party ones.
* **Bloat Check**: Mention if the library has a high method count or a lighter alternative exists.
* **Format**: Always provide the implementation string for `build.gradle.kts`.
* **Versions**: Always apply versioning through the `libs.versions.toml` catalog.
* **Version Bounding**: Always apply version bounding to the library, exclusive of the next major update, e.g. `"[1.0.1, 1.1.0)"`.
* **Cleanup**: Proactively remove unused dependencies and their corresponding entries in the version catalog.
* **Transitive Audit**: Briefly check for heavy transitive dependencies that might cause bloat or version conflicts.