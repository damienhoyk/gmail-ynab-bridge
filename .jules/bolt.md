## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2025-05-16 - Hot Path Data Processing in TransactionMatcher
**Learning:** Parsing transactions from emails is a hot-path operation where object allocations (lists, maps, formatters) quickly add up. Pre-calculating capturing group indices and using manual character loops for numeric parsing significantly reduces overhead and GC pressure.
**Action:** For high-frequency data parsing, pre-calculate indices, avoid intermediate collections, and prefer manual character loops over repeated string replacements for numeric data.
