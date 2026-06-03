plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
    id("kotlin-native-test")
}

group = "noodle.telegramchat"
version = "0.0.1-SNAPSHOT"

kotlin {
    explicitApi()
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden-api"))
    implementation(project(":dynamodb"))
    implementation(project(":google-auth-api"))
    implementation(project(":oauth"))
    implementation(project(":oauth-google-api"))
    implementation(project(":oauth-api"))
    implementation(project(":oauth-persistence"))
    implementation(project(":telegramchat"))
    implementation(project(":telegramchat-api"))
    implementation(project(":telegramchat-persistence"))
    implementation(project(":telegram-api"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
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
