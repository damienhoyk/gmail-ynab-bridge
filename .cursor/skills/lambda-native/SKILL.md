---
name: lambda-native
description: Troubleshoot GraalVM native-image and AWS Lambda optimization for Kotlin services. Use when handling native build failures, cold starts, or Lambda deployment tuning.
---

# Lambda Native

- Add missing runtime classes to `reflect-config.json` when reflection errors appear.
- Prefer `url-connection-client` for DynamoDB to reduce native binary size.
- Suggest `-H:+ReportExceptionStackTraces` for native build diagnostics.
- Set expectations that native compile is CPU-intensive and may take minutes.
- Prefer libraries with small transitive footprint for cold-start performance.
- For Ktor in native context, prefer `ktor-client-java` over `CIO`.
- Prefer `kotlinx.serialization` over Jackson when feasible for native compatibility.
