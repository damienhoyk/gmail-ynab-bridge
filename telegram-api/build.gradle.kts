plugins {
    id("kotlin-jvm")
}

group = "noodle.telegram"

kotlin {
    explicitApi()
}
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(project(":bitwarden-api"))
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
