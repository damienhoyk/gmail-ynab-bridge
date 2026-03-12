plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":bridge-dynamodb"))
    implementation(project(":dynamodb"))
    implementation(project(":email-dynamodb"))
    implementation(project(":gmail-pubsub"))
    implementation(project(":google-gmail"))
    implementation(project(":google-gmail-client"))
    implementation(project(":google-auth-client"))
    implementation(project(":security"))
    implementation(project(":security-client"))
    implementation(project(":security-dynamodb"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bitwarden.secrets)
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:url-connection-client")
    runtimeOnly(libs.logback)
}
