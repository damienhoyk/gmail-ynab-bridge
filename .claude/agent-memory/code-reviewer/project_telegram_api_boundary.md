---
name: project-telegram-api-boundary
description: telegram-api integration module currently violates dependency direction by implementing telegramchat core port
metadata:
  type: project
---

`telegram-api` is structured as a shared integration module (`noodle.telegram.infrastructure.api`) but `KtorTelegramBotClient` implements `noodle.telegramchat.core.port.TelegramBotClient`, forcing `telegram-api/build.gradle.kts` to depend on `:telegramchat` (app core).

**Why:** project-packaging.md forbids integrations from knowing application types — each app must own its adapter that wires the integration to its own port. Same issue pattern as gmailsync vs gmail-api (which is done correctly: `gmailsync-api` owns the port impl, `gmail-api` is pure Gmail).

**How to apply:** If asked to fix or extend telegram bot work, the correct shape is: `telegram-api` exposes a plain `KtorTelegramBotClient` (no port impl), and a `KtorTelegramBotClientAdapter` lives in `telegramchat-api` implementing the port. See [[project-module-structure]].
