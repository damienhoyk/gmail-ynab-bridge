## 2025-05-15 - Redundant String Processing and Regex Pre-compilation
**Learning:** In the `google-gmail` module, `stripHtml()` was found to handle whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Additionally, calling `.toRegex()` inside extension functions causes the regex to be re-compiled on every invocation, which is inefficient for static patterns.
**Action:** Always check if multiple string processing steps overlap in functionality. Pre-compile static Regex objects as private top-level constants to avoid redundant compilation.

## 2025-05-15 - List Iteration and View vs Copy
**Learning:** Using `reversed()` on a list creates a new list copy, which is inefficient when only read-only reverse iteration is needed.
**Action:** Use `asReversed()` to get a reversed view of the list instead of a copy when possible.

## 2025-05-15 - Redundant List Allocation in TransactionMatcher
**Learning:** Using `order.toList().indexOf(...)` for a small Set creates an unnecessary List allocation. `Iterable.indexOf` is available directly on Sets and avoids this extra memory allocation, improving performance, especially if such objects are created frequently.
**Action:** Use `.indexOf()` directly on Set/Iterable objects instead of calling `.toList()` first.
## 2026-05-04 - Sequence Processing in Kotlin Pipelines
**Learning:** In Kotlin data processing pipelines, using `asSequence()` before chaining operations like `mapNotNull` and combining transformations into terminal operations (e.g., `joinToString`) avoids allocating intermediate lists. This measurably improves performance by preventing unnecessary memory copies, particularly in models like `GmailMessage` where large part payloads are flattened and processed.
**Action:** When transforming lists with multiple sequential operations (e.g., map, filter) in hot paths, consider inserting `.asSequence()` to prevent intermediate allocations, especially if the pipeline results in a terminal collection or string.
## 2026-06-08 - Eager System Clock Reads in High-Frequency Parsing
**Learning:** In `TransactionMatcher.parse`, calling `LocalDate.now()` unconditionally for every transaction parsed introduces unnecessary object allocations and system clock reads, even when all required date fields are already provided in the parsed string.
**Action:** Always defer evaluating `LocalDate.now()` or checking the system clock until it is strictly necessary to resolve missing date components.
