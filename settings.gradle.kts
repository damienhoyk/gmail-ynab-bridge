dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include(":oauth-event-handler-core")
include(":bridge-repository")
include(":dynamodb")
include(":email-dynamodb-event-handler")
include(":email-repository")
include(":gmail-pubsub-event-handler")
include(":gmail-ynab-job")
include(":google-auth")
include(":google-gmail")
include(":google-oauth-event-handler")
include(":security")
include(":security-repository")
include(":telegram-bot")
include(":telegram-bot-event-handler")
include(":user-repository")
include(":ynab")
include(":ynab-oauth-event-handler")

rootProject.name = "gmail-ynab-bridge"