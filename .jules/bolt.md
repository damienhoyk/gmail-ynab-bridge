## 2025-05-15 - [Build/Test Dependency Bottleneck]
**Learning:** The project relies on private GitHub Packages (Bitwarden SDK) which requires 'github.user' and 'github.key' Gradle properties. Without these, even unrelated modules' tests may fail to run due to dependency resolution of the 'security' module.
**Action:** When working on performance optimizations in modules like 'google-gmail', verify logic manually or via standalone scripts if the full Gradle test suite is blocked by missing credentials.

## 2025-05-15 - [Redundant String Processing in Gmail]
**Learning:** The `stripHtml()` extension function already collapses all whitespace including line breaks. Preceding it with `stripLineBreaks()` is redundant and adds unnecessary regex overhead.
**Action:** Always check `stripHtml()` implementation before adding line-break specific processing.
