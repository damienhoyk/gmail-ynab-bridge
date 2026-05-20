dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Core Modules
include(":google-gmail")
include(":security")
include(":gmail-pubsub")
include(":ynabsync")
include(":telegram-bot")

// Infrastructure Adapters
include(":bridge-dynamodb")
include(":email-dynamodb")
include(":google-auth-client")
include(":google-gmail-client")
include(":security-client")
include(":security-dynamodb")
include(":telegram-bot-client")
include(":user-dynamodb")
include(":ynab-auth-client")
include(":ynab-client")

// Common Libraries
include(":bitwarden")
include(":dynamodb")

// Event Handlers
include(":gmail-pubsub-event-handler")
include(":google-oauth-event-handler")
include(":telegram-bot-event-handler")
include(":ynab-email-event-handler")
include(":ynab-oauth-event-handler")

rootProject.name = "gmail-ynab-bridge"
