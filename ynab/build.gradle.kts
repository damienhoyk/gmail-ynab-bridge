plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":security"))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    testImplementation(platform(libs.aws.sdk.dependencies))
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
    testImplementation(project(":google-auth"))
    testImplementation("com.bitwarden:sdk-secrets:1.0.1")
    testImplementation("software.amazon.awssdk:dynamodb")
    testImplementation("software.amazon.awssdk:secretsmanager")
}