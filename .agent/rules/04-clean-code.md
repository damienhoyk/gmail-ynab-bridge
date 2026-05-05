---
trigger: always_on
---

# Clean Code and Readability

- Adhere to the Single Responsibility Principle (SRP) and keep functions strictly focused on one task.
- Eliminate duplicated logic by extracting reusable utility functions or shared services.
- Avoid boilerplate code by utilizing language-native features and concise syntax.
- Fail fast by validating inputs immediately at the system boundaries.
- Use descriptive, intention-revealing names for variables, functions, and classes.
- Avoid extracting helper functions that are only used once; prefer inlining unless extraction improves reuse or isolates meaningful domain intent.
- Write self-documenting code where the logic is clear enough to minimize inline comments.
- Restrict comments to explaining the underlying business reasons for a decision, not the mechanics of the code.
- Maintain consistent formatting, grouping related logic together with clear vertical spacing.