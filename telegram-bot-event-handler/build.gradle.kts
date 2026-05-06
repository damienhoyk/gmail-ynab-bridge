plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.chat"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":dynamodb"))
    implementation(project(":email-dynamodb"))
    implementation(project(":google-auth-client"))
    implementation(project(":google-gmail"))
    implementation(project(":google-gmail-client"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":security-dynamodb"))
    implementation(project(":telegram-bot"))
    implementation(project(":telegram-bot-client"))
    implementation(project(":user-dynamodb"))
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
