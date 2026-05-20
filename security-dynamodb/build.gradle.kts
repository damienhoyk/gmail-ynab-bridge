plugins {
    id("kotlin-jvm")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":security"))
    implementation(project(":telegramchat"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    testImplementation("software.amazon.awssdk:signin")
}
