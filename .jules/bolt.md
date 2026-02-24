## 2025-05-15 - [Redundant string processing in Gmail message cleaning]
**Learning:** The `stripHtml()` extension function already handles whitespace collapsing (including line breaks via `\s+`), making preceding calls to `stripLineBreaks()` redundant. Also, pre-compiling Regex objects as constants significantly reduces the overhead of repeated string cleaning operations.
**Action:** Always check if a cleaning/formatting pipeline has overlapping steps and pre-compile Regexes that are used in hot paths like message part processing.

## 2025-05-15 - [Efficient list iteration in Kotlin]
**Learning:** Using `asReversed()` provides a reversed view of a list without creating a new list allocation, which is much more efficient than `reversed()` when the original list doesn't need to be preserved as a separate copy.
**Action:** Prefer `asReversed()` over `reversed()` for read-only iterations or when adding elements to another collection in reverse order.
