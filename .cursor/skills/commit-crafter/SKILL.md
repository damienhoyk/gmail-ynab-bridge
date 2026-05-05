---
name: commit-crafter
description: Draft high-quality Conventional Commit messages and run a safe commit flow. Use when the user asks to commit, write a commit message, or prepare a release commit.
---

# Commit Crafter

Create commit messages that explain user value and intent.

## Workflow

1. Run git audit first using `.cursor/skills/git-audit/scripts/audit.sh`.
2. Inspect staged diff and pick one Conventional Commit type.
3. Select scope from affected modules or architectural boundary.
4. Draft a subject line in format: `type(scope): summary`.
5. Add a short body when context is non-obvious.
6. Ask for user confirmation before executing commit.

## Commit Types

- `feat`: new behavior
- `fix`: bug resolution
- `refactor`: internal redesign with same behavior
- `test`: test additions/updates
- `docs`: documentation only
- `chore`: build/tooling/maintenance

## Message Quality Bar

- Focus on why and impact, not only file changes.
- Keep subject concise and actionable.
- Avoid vague scopes like `misc`.

## Examples

See [examples.md](examples.md).
