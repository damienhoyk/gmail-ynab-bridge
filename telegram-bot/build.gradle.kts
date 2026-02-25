plugins {
    id("kotlin-jvm")
}

group = "noodle.telegram.bot"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":security"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.logback)
}
