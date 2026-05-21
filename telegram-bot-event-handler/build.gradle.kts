plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.telegramchat"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":security-bitwarden"))
    implementation(project(":dynamodb"))
    implementation(project(":google-auth-api"))
    implementation(project(":gmailsync"))
    implementation(project(":google-gmail-api"))
    implementation(project(":oauth"))
    implementation(project(":oauth-api"))
    implementation(project(":oauth-dynamodb"))
    implementation(project(":telegramchat"))
    implementation(project(":telegramchat-dynamodb"))
    implementation(project(":telegram-bot-api"))
    implementation("io.ktor:ktor-client-auth")
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bitwarden.secrets)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}

tasks.withType<Test>().configureEach {
    environment("GOOGLE_REDIRECT_URI", "http://localhost")
    environment("YNAB_REDIRECT_URI", "http://localhost")
}
