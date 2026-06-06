---
name: dynamodb-token-repository-clash
description: Two same-named DynamoDbTokenRepository classes (oauth vs telegramchat persistence) clash in TelegramBotHandler
metadata:
  type: project
---

`DynamoDbTokenRepository` exists in BOTH `noodle.oauth.infrastructure.persistence` and `noodle.telegramchat.infrastructure.persistence`. Both are instantiated in `telegramchat-bootstrap/.../TelegramBotHandler.kt`.

**Why:** The oauth one backs the google `TokenService` (security token repo); the telegramchat one backs `TelegramBotService`. Distinct types, identical simple name.

**How to apply:** When reviewing/editing TelegramBotHandler, only one of the two can be a plain import — the other MUST stay an inline FQN, or both stay FQN. Watch for a dead plain import left behind when both use sites get qualified inline (saw this in commit 6acb036 on cleanup/qualifiers). Verify each repo is wired to the correct service: securityTokenRepository(oauth) -> TokenService.tokenRepository; telegramTokenRepository -> TelegramBotService.tokenRepository.
