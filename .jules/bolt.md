## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2026-03-02 - Hot-path Transaction Parsing Optimization
**Learning:** In `TransactionMatcher`, repeated collection allocations (`map`, `setOf`) and regex-based string replacements (`replace`) in the `parse` method significantly increase GC pressure and CPU overhead. Pre-calculating regex group indices in the constructor and using a manual character loop for numeric extraction avoids these allocations.
**Action:** Always hoist index calculations and constant configurations to the constructor for classes used in hot-path processing. Prefer manual character loops over `String.replace` for simple numeric extraction.
