plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.tokenrefresher.infrastructure.api.ynab"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":tokenrefresher"))
    implementation(project(":ynab-auth-api"))
    implementation(project(":oauth2-api"))
    implementation(project(":ktor"))
    implementation(libs.bundles.ktor.client)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
