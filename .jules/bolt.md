## 2026-02-22 - [Regex and Collection Optimizations]
**Learning:** Pre-compiling Regex objects and using collection views (asReversed) are simple but effective ways to reduce allocations and CPU usage in hot paths like message processing. In this codebase, Gmail message parsing was recompiling regexes multiple times per message part.
**Action:** Always check for .toRegex() calls in extension functions or loops. Prefer asReversed() over reversed() when only iteration is needed.
