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
include(":oauth")
include(":gmailsync")
include(":ynabsync")
include(":telegramchat")

// Infrastructure Adapters
include(":gmailsync-persistence")
include(":ynabsync-persistence")
include(":google-auth-api")
include(":google-gmail-api")
include(":oauth-api")
include(":oauth-persistence")
include(":telegram-bot-api")
include(":telegramchat-persistence")
include(":ynab-auth-api")
include(":ynab-api")

// Common Libraries
include(":bitwarden")
include(":dynamodb")
include(":serialization")

// Event Handlers
include(":gmailsync-handler")
include(":oauth-google-handler")
include(":telegramchat-handler")
include(":ynabsync-handler")
include(":oauth-ynab-handler")

rootProject.name = "gmail-ynab-bridge"
