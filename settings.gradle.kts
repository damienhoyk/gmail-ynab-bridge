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
include(":ktor")
include(":bitwarden-api")
include(":dynamodb")
include(":oauth2-api")
include(":uri")

// Bootstrap
include(":gmailsync-bootstrap")
include(":oauth-google-bootstrap")
include(":telegramchat-bootstrap")
include(":ynabsync-bootstrap")
include(":oauth-ynab-bootstrap")
include(":tokenrefresher-bootstrap")

// Token Refresher
include(":tokenrefresher")
include(":tokenrefresher-google-api")
include(":tokenrefresher-ynab-api")
include(":tokenrefresher-persistence")

// Architecture tests
include(":architecture-test")

rootProject.name = "gmail-ynab-bridge"
