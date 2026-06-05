plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.ynab.auth"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":ktor"))
    implementation(libs.bundles.ktor.client)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
