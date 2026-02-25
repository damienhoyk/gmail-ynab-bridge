plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function-native")
}

group = "noodle.event.handler"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":oauth-event-handler-core"))
    implementation(project(":security"))
    implementation(project(":ynab"))
    implementation(libs.bundles.aws.lambda)
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.logback)
}

tasks.withType<Test>().configureEach {
    environment("REDIRECT_URI", "http://localhost")
    environment("SECRET_ID", "dummy-secret")
}