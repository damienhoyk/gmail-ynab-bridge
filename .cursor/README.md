# Cursor Guidance Map

This directory contains project-specific AI guidance in Cursor-native formats.

## Structure

- `rules/`: Always-on and file-scoped project rules (`.mdc`).
- `skills/`: Reusable workflows triggered by task intent (`SKILL.md`).

## Rule Precedence

Rules are numbered by priority and should be interpreted top-down:

1. `01-core-directives.mdc`
2. `02-architecture-checklist.mdc`
3. `03-performance-checklist.mdc`
4. `04-clean-code-checklist.mdc`
5. `05-references.mdc`

## Skill Selection

- Use `git-audit` to inspect repository state and architecture/test risks.
- Use `commit-crafter` to draft commit messages and run a safe commit flow.
- Use `dependency-guard` when adding/updating dependencies.
- Use `lambda-native` for GraalVM native image and AWS Lambda tuning.

## Notes

- `.agent/` remains as legacy guidance and examples.
- Prefer relative links so guidance is portable across machines.
