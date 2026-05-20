plugins {
    id("kotlin-jvm")
}

group = "noodle.telegramchat"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":google-gmail"))
    implementation(project(":security"))
    implementation(platform(libs.ktor.dependencies))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
    testImplementation("io.ktor:ktor-client-mock")
    testImplementation(project(":bitwarden"))
    testImplementation(libs.slf4j.simple)
}
