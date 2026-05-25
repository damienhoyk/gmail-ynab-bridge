---
name: project-module-structure
description: Bootstrap module layout and naming after the *-handler to *-bootstrap compliance refactor
metadata:
  type: project
---

Five `*-bootstrap` modules are the composition roots / Lambda entry points: `gmailsync-bootstrap`, `ynabsync-bootstrap`, `telegramchat-bootstrap`, `oauth-google-bootstrap`, `oauth-ynab-bootstrap`. oauth has two qualified bootstraps (`google`, `ynab`).

Entry classes are still named `*Handler` (e.g. `GmailPubsubHandler`, `YnabOAuthHandler`) but live in `noodle.<app>.bootstrap[.<qualifier>]` packages and are registered in each module's `src/main/resources/META-INF/native-image/reflect-config.json`.

**Why:** project-packaging.md requires bootstrap to be the only module type permitted to import peer adapter modules.

**How to apply:** When reviewing, the entry class FQN must match across: directory name, settings.gradle.kts include, package declaration, reflect-config.json `name`. Bootstrap modules legitimately import adapters from multiple applications (cross-app) — that is allowed for composition roots, not a dependency violation.
