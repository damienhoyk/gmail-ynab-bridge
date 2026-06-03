plugins {
    id("kotlin-jvm")
}

group = "noodle.telegramchat"
version = "0.0.1-SNAPSHOT"

kotlin {
    explicitApi()
}

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(project(":bitwarden-api"))
    testImplementation(libs.slf4j.simple)
}
