plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":bitwarden"))
    implementation(project(":security"))
    implementation("io.ktor:ktor-client-auth")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation(libs.bundles.ktor.client)
}
