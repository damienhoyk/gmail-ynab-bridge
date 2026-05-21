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
include(":security")
include(":gmailsync")
include(":ynabsync")
include(":telegramchat")

// Infrastructure Adapters
include(":bridge-dynamodb")
include(":email-dynamodb")
include(":google-auth-api")
include(":google-gmail-api")
include(":security-client")
include(":security-dynamodb")
include(":telegram-bot-api")
include(":telegramchat-dynamodb")
include(":user-dynamodb")
include(":ynab-auth-api")
include(":ynab-api")

// Infrastructure Adapters (continued)
include(":security-bitwarden")

// Common Libraries
include(":dynamodb")

// Event Handlers
include(":gmailsync-handler")
include(":google-oauth-event-handler")
include(":telegram-bot-event-handler")
include(":ynabsync-handler")
include(":ynab-oauth-event-handler")

rootProject.name = "gmail-ynab-bridge"
