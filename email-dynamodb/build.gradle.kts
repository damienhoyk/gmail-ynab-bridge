plugins {
    id("kotlin-jvm")
}

group = "noodle.gmailsync"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":ynabsync"))
    implementation(project(":gmailsync"))
    implementation(project(":dynamodb"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation(libs.kotlinx.coroutines)
    testImplementation("software.amazon.awssdk:signin")
}
