plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlinx.coroutines)
    implementation("com.bitwarden:sdk-secrets:1.0.1")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
}