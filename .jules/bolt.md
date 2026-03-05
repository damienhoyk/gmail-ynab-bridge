## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2025-05-16 - Transaction Parsing Optimization
**Learning:** In the `ynab-email` module, `TransactionMatcher.parse` is a high-frequency method. Pre-calculating capturing group indices in the constructor and using a manual character loop for currency parsing significantly reduces allocations and CPU cycles compared to repeated index lookups and `String.replace`.
**Action:** For hot-path data extraction, pre-calculate index mappings and prefer manual loops over multiple string transformations.
