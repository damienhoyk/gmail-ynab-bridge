plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.telegramchat"
version = "0.0.1-SNAPSHOT"

kotlin {
    explicitApi()
}

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":telegramchat"))
    implementation(project(":gmailsync"))
    implementation(project(":gmail-api"))
    implementation(project(":telegram-api"))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    implementation(libs.kotlinx.coroutines)
}
