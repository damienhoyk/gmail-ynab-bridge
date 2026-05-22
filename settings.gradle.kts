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

// Bootstrap
include(":gmailsync-bootstrap")
include(":oauth-google-bootstrap")
include(":telegramchat-bootstrap")
include(":ynabsync-bootstrap")
include(":oauth-ynab-bootstrap")

rootProject.name = "gmail-ynab-bridge"
