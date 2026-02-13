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

include(":authorization-function")
include(":google-auth")
include(":google-authorization-function")
include(":google-gmail")
include(":gmail-ynab-job")
include(":gmail-ynab-bot-function")
include(":gmail-ynab-bridge-function")
include(":security")
include(":ynab")
include(":ynab-authorization-function")

rootProject.name = "gmail-ynab-bridge"