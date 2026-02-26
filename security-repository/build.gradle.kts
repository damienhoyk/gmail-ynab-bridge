plugins {
    id("kotlin-jvm")
}

group = "noodle.security"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":dynamodb"))
	implementation(libs.kotlinx.coroutines)
    implementation(project(":security"))
    implementation("software.amazon.awssdk:dynamodb")
    runtimeOnly(libs.logback)
}
