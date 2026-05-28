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
include(":gmailsync-api")
include(":gmailsync-persistence")
include(":ynabsync-persistence")
include(":google-auth-api")
include(":gmail-api")
include(":oauth-api")
include(":oauth-google-api")
include(":oauth-persistence")
include(":oauth-ynab-api")
include(":telegram-api")
include(":telegramchat-api")
include(":telegramchat-persistence")
include(":ynab-api")
include(":ynab-auth-api")
include(":ynabsync-api")

// Common Libraries
include(":bitwarden-api")
include(":dynamodb")

// Bootstrap
include(":gmailsync-bootstrap")
include(":oauth-google-bootstrap")
include(":telegramchat-bootstrap")
include(":ynabsync-bootstrap")
include(":oauth-ynab-bootstrap")

rootProject.name = "gmail-ynab-bridge"
