---
name: cross-app-oauth-adapters
description: LoginIdProvider/OAuth2Client adapters that implement oauth ports but live in other apps' infra modules
metadata:
  type: project
---

Adapters implementing oauth's `noodle.oauth.core.port.*` ports belong in the oauth app's own infra tree. Current/known state:
- `KtorYnabLoginIdProvider` now lives in `oauth-ynab-api` (`noodle.oauth.infrastructure.api.ynab`), alongside its sibling `KtorYnabAuthClient`. (Moved on branch `align-loginid-providers` from `ynabsync-api`/`noodle.ynabsync.infrastructure.api` — verify merged before relying on this.) It depends on `:oauth`, `:oauth-api`, `:ynab-auth-api`, `:ynab-api`. Uses the `noodle.ynab.infrastructure.api.model.YnabUser` model in `ynab-api`.
- `KtorGoogleLoginIdProvider` lives in `oauth-google-api` (`noodle.oauth.infrastructure.api.google`).
- `gmailsync-api` (`noodle.gmailsync.infrastructure.api.KtorGoogleOAuth2Client`) still imports `oauth.infrastructure.api.TokenInfoResponse` — remaining pre-existing drift, out of scope unless the change targets it.

**Why:** symmetry between the two oauth login-id adapters; an adapter implementing an oauth output port should not sit under another application's module.

**How to apply:** treat the gmailsync-api/TokenInfoResponse case as pre-existing drift. The ynab provider relocation is the canonical pattern to mirror going forward.
