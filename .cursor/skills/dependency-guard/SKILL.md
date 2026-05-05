---
name: dependency-guard
description: Evaluate and add dependencies safely in Gradle Kotlin DSL projects. Use when editing build files, adding libraries, or updating version catalog entries.
---

# Dependency Guard

When adding or updating dependencies:

- Verify compatibility with current Java/Kotlin and framework versions.
- Prefer official JetBrains/Google libraries where fit-for-purpose.
- Check for heavy transitive dependencies and version conflicts.
- Add dependency coordinates for `build.gradle.kts`.
- Manage versions in `libs.versions.toml`.
- Use bounded version ranges when policy requires controlled upgrades.
- Remove unused dependencies and stale catalog entries.
