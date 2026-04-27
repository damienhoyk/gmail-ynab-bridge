plugins {
    id("kotlin-jvm")
}

group = "noodle.bridge"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":gmail-pubsub"))
    implementation(project(":ynab-email"))
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    runtimeOnly(libs.logback)
    testImplementation("software.amazon.awssdk:signin")
}
