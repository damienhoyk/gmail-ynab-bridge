plugins {
    id("kotlin-jvm")
}

group = "noodle.google.auth"

kotlin {
    explicitApi()
}
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
