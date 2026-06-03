plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

group = "noodle.gmailsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":gmailsync"))
    implementation(project(":gmail-api"))
    implementation(project(":google-auth-api"))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    implementation(libs.kotlinx.coroutines)
}
