plugins {
    id("kotlin-jvm")
}

group = "noodle.client"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(platform(libs.ktor.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":security"))
    implementation(project(":security-repository"))
    implementation(project(":telegram-bot"))
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-auth")
    implementation("software.amazon.awssdk:dynamodb")
}
