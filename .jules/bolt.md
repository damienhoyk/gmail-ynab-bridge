## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2025-05-15 - Pre-calculating Regex Capture Group Mapping
**Learning:** O(n) lookups of the capture group set inside the fast-path method `.parse()` are a performance bottleneck in `ynab-email/TransactionMatcher.kt`.
**Action:** Pre-calculate capturing group indices mappings in the constructor, since the regex pattern and required capturing groups do not change dynamically. Avoid dynamic set lookups on every function invocation.