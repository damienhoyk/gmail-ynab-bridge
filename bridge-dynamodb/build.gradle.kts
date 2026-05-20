plugins {
    id("kotlin-jvm")
}

group = "noodle.bridge"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":dynamodb"))
    implementation(project(":gmailsync"))
    implementation(project(":ynabsync"))
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    runtimeOnly(libs.logback)
    testImplementation("software.amazon.awssdk:signin")
}
