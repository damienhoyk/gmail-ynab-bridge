plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.repository"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}