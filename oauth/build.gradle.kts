plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
}

kotlin {
    explicitApi()
}

group = "noodle.oauth"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
}
