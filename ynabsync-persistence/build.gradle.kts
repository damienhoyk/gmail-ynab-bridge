plugins {
    id("kotlin-jvm")
}

group = "noodle.ynabsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":ynabsync"))
    implementation(project(":dynamodb"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    runtimeOnly(libs.logback)
    testImplementation("software.amazon.awssdk:signin")
}
