## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2025-05-15 - Pre-calculating Indices for Data Processing Hot Paths
**Learning:** In the `ynab-email` module, `TransactionMatcher.parse` was dynamically converting `Set<RegexGroup>` to a `List` and performing O(n) `indexOf` lookups to find capturing group indices on every transaction parse.
**Action:** Move static collection transformations and index lookups into the constructor/init block for data processing classes. Cache the resulting primitive index values as properties to eliminate dynamic memory allocations and O(n) lookups in high-frequency methods.
