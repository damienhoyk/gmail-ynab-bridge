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
include(":bridge-dynamodb")
include(":bridge-repository")
include(":bitwarden")
include(":dynamodb")
include(":email-dynamodb")
include(":email-repository")
include(":gmail-pubsub")
include(":gmail-pubsub-event-handler")
include(":google-auth")
include(":google-auth-client")
include(":google-client-factory")
include(":google-gmail")
include(":google-gmail-client")
include(":google-oauth-event-handler")
include(":security")
include(":security-client")
include(":security-dynamodb")
include(":security-repository")
include(":telegram-bot")
include(":telegram-bot-client")
include(":telegram-client-factory")
include(":telegram-bot-event-handler")
include(":user-dynamodb")
include(":user-repository")
include(":ynab")
include(":ynab-auth")
include(":ynab-auth-client")
include(":ynab-client")
include(":ynab-client-factory")
include(":ynab-email")
include(":ynab-email-event-handler")
include(":ynab-oauth-event-handler")

rootProject.name = "gmail-ynab-bridge"
