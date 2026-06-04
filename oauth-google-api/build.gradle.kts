plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.oauth"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":oauth"))
    implementation(project(":google-auth-api"))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
