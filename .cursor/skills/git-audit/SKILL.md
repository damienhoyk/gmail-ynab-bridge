---
name: git-audit
description: Audit repository status, staged changes, and architecture/test risk before committing. Use when the user asks to check git status, review staged work, or validate commit readiness.
---

# Git Audit

Run this workflow before proposing or creating commits.

## Steps

1. Run `.cursor/skills/git-audit/scripts/audit.sh`.
2. Read `### Layer Analysis ###` and flag:
   - Domain and infrastructure changed together.
   - Domain changes without tests.
3. Read `### Kotlin TODO Check ###` and `### Debt Check ###`.
4. If `large-change-warning:true`, suggest splitting the work.
5. Summarize risks first, then readiness.

## Required Warnings

- If `domain-files > 0` and `infrastructure-files > 0`:
  - "This change spans Domain and Infrastructure. Consider splitting for cleaner review and architecture integrity."
- If `domain-files > 0` and `test-files == 0`:
  - "Domain logic changed without tests. Add or update tests before commit."

## Output Format

- **Repo state**: branch + staged/unstaged summary.
- **Architecture risk**: layer and boundary notes.
- **Code health risk**: TODO/FIXME/manual logging/large change.
- **Recommendation**: proceed, split, or add tests first.
