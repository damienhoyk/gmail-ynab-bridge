plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}