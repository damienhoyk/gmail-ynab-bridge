plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":authorization-function"))
    implementation(project(":google-auth"))
    implementation(project(":security"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.aws.lambda.core)
    implementation("software.amazon.awssdk:dynamodb")
    runtimeOnly(libs.logback)
}
