---
name: lambda-native
description: Trigger when troubleshooting GraalVM native image builds or AWS Lambda deployment.
---
# Lambda Native

- **Reflection**: If a class isn't being found at runtime, suggest adding it to `reflect-config.json`.
- **DynamoDB**: Remind the user to use the `url-connection-client` instead of the default Netty client to keep the binary size small.
- **Build Command**: Always suggest the `-H:+ReportExceptionStackTraces` flag for easier debugging of build-time failures.
- **Performance Check**: Prioritize libraries with low coldstart impact and small binary footprints. Avoid heavy transitive chains.
- **HTTP Client Engine**: For Ktor, prefer the `Java` engine (`ktor-client-java`) over `CIO` to minimize binary size and improve coldstart performance.
- **Serialization**: Prefer `kotlinx.serialization` over Jackson where possible for smaller footprints and better Native compatibility.