plugins {
    id("kotlin-jvm")
}

group = "noodle.email"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(platform(libs.aws.sdk.dependencies))
    implementation(project(":dynamodb"))
	implementation(libs.kotlinx.coroutines)
    implementation("software.amazon.awssdk:dynamodb")
    runtimeOnly(libs.logback)
}
