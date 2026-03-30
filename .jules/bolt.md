## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2025-05-15 - Pre-calculating Regex Group Indices
**Learning:** In hot-path parsing logic (like `TransactionMatcher.parse`), dynamically allocating sets and iterating through mappings (e.g., `setOf("ENTIRE_MATCH") + order` and `RegexGroup.entries.map`) on every invocation causes unnecessary memory allocation and CPU overhead.
**Action:** Pre-calculate capturing group indices as primitive properties during object construction to eliminate redundant allocations and allow direct O(1) lookups during execution.
