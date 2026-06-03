plugins {
    id("kotlin-jvm")
}

kotlin {
    explicitApi()
}

group = "noodle.oauth"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":oauth"))
    implementation(project(":dynamodb"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.logback)
    testImplementation("software.amazon.awssdk:signin")
}
