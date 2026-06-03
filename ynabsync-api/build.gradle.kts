plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

kotlin {
    explicitApi()
}

group = "noodle.ynabsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":ynabsync"))
    implementation(project(":gmail-api"))
    implementation(project(":ynab-api"))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    implementation(libs.kotlinx.coroutines)
    implementation(libs.jakarta.mail)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
