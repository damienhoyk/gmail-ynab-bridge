plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(project(":security"))
    implementation("io.ktor:ktor-client-auth")
    testImplementation(platform(libs.aws.sdk.dependencies))
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
    testImplementation(project(":google-auth"))
    testImplementation("software.amazon.awssdk:dynamodb")
    testImplementation("software.amazon.awssdk:secretsmanager")
}