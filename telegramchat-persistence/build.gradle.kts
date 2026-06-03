plugins {
    id("kotlin-jvm")
}

group = "noodle.telegramchat"
version = "0.0.1-SNAPSHOT"

kotlin {
    explicitApi()
}

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":telegramchat"))
    implementation(project(":dynamodb"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    testImplementation("software.amazon.awssdk:signin")
}
