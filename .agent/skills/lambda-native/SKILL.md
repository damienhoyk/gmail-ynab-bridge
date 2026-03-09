---
name: lambda-native
description: Trigger when troubleshooting GraalVM native image builds or AWS Lambda deployment.
---
# Lambda Native

* **Reflection**: If a class isn't being found at runtime, suggest adding it to `reflect-config.json`.
* **DynamoDB**: Remind the user to use the `url-connection-client` instead of the default Netty client to keep the binary size small.
* **Build Command**: Always suggest the `-H:+ReportExceptionStackTraces` flag for easier debugging of build-time failures.