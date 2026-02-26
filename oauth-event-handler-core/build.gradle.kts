plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.event.handler"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":security"))
    implementation(project(":security-repository"))
    implementation(project(":user-repository"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.aws.lambda.core)
    implementation(libs.aws.lambda.events)
    implementation(libs.kotlinx.coroutines)
    implementation("com.bitwarden:sdk-secrets:1.0.1")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:secretsmanager")
    runtimeOnly(libs.logback)
}
