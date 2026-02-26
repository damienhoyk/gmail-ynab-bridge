plugins {
    id("kotlin-jvm")
}

group = "noodle.chat"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(project(":bitwarden"))
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.slf4j.api)
}
