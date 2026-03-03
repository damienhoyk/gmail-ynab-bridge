## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2026-03-03 - Optimization of High-Frequency Parsing Logic
**Learning:** In `TransactionMatcher.parse`, repeated set creation and list lookups for regex group indices were identified as a bottleneck. Moving these calculations to the constructor (pre-calculating indices) significantly reduces per-call overhead. Additionally, using `LocalDate.toString()` for ISO-8601 formatting is more efficient than `DateTimeFormatter`.
**Action:** Pre-calculate static mappings in constructors for hot-path data processing and prefer built-in formatting for standard ISO patterns.
