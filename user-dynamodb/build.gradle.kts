plugins {
    id("kotlin-jvm")
}

group = "noodle.user"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":security"))
    implementation(project(":telegram-bot"))
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    runtimeOnly(libs.logback)
    testImplementation("software.amazon.awssdk:signin")
}
