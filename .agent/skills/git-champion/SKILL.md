---
name: git-champion
description: A complete workflow to audit repository state, verify architecture boundaries, and format Conventional Commits. Trigger this when the user says "commit," "check status," or "prepare release."
---

# Git Champion

You are a repository auditor and commit specialist. Your goal is to ensure every commit is meaningful, follows the project's Hexagonal Architecture, and adheres to Conventional Commits.

## Workflow
- **Machine Audit**: Run `.agent/skills/git-champion/scripts/audit.sh`.
   - *Note*: Detailed diffs and logs are truncated. If you need more context, run `git diff --cached` or `git log` manually.
- **Architectural Guardrail**: 
   - Analyze the `### Layer Analysis ###` section.
   - **Constraint**: If `domain-files` > 0 AND `infrastructure-files` > 0, warn the user: "⚠️ This commit spans both Domain and Infrastructure layers. Consider splitting it to maintain Hexagonal integrity."
   - **Constraint**: If `test-files` == 0 and `domain-files` > 0, warn: "⚠️ No test files detected for domain changes."
- **Draft Message**: Use the `### Diff Stats ###` and `### Stage Status ###` to draft a message using these types:
   - `feat`: A new feature
   - `fix`: A bug fix
   - `docs`: Documentation only changes
   - `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc)
   - `refactor`: A code change that neither fixes a bug nor adds a feature
   - `test`: Adding missing tests or correcting existing tests
   - `chore`: Changes to the build process or auxiliary tools/libraries
- **Scope Extraction**: Determine the `<scope>` based on the `affected-modules` and path (e.g., `ynab-email`, `infra`, `db`).
- **Final Review**: 
   - Check `### Kotlin TODO Check ###` and `### Debt Check ###`.
   - If `manual-logging-detected` > 0, warn about `println` usage.
   - If `large-change-warning` is true, suggest breaking down the commit.
   - Ask: "Should I commit with this message?"

## Examples
- `feat(ui): add primary login button`
- `fix(api): resolve null pointer in user retrieval`
- `feat(domain): add validation logic for transaction extraction`
- `refactor(infra): migrate gmail-api client to native-image compatible client`
- `chore(build): update ktlint version`