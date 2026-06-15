plugins {
    id("kotlin-jvm")
}

group = "noodle"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":tokenrefresher"))
    implementation(project(":dynamodb"))
    implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    testImplementation("software.amazon.awssdk:signin")
}
