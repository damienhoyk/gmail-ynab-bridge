plugins {
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-jvm")
    id("kotlin-function")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":authorization-function"))
    implementation(project(":security"))
    implementation(project(":ynab"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.aws.lambda.core)
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.logback)
}
