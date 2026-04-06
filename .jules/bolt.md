## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.
## 2025-05-15 - [Pre-calculating Capturing Group Indices in TransactionMatcher]
**Learning:** In hot-path data processing like `TransactionMatcher.parse`, dynamic allocations such as `setOf` and list mapping over enums (`RegexGroup.entries.map`) create unnecessary GC pressure. Since the capturing group ordering (`order`) is defined once at initialization, the mapping of `RegexGroup` to match index is entirely static for a given matcher instance.
**Action:** Always pre-calculate and cache mappings (like `order.indexOf(...)`) as private primitive properties in the constructor for hot-path matching. This eliminates per-invocation allocations and intermediate collections, significantly improving parser performance.
